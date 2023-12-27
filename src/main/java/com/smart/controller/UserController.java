package com.smart.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private ContactRepository contactRepos;

    @Autowired
    private MyOrderRepository myOrderRepos;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // this method will run every time for every handler to add user data for sending to view
    @ModelAttribute
    public void addCommonData(Model model, Principal principal)
    {
        String userName = principal.getName();
        // get the user using username(i.e; Email)
//        System.out.println("USERNAME: "+userName);

        User user = userRepos.getUserByUserName(userName);
//        System.out.println(user);

        model.addAttribute("user",user);
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal)
    {
//        String userName = principal.getName();
//        System.out.println(userName);
        model.addAttribute("title", "User Dashboard");
        return "normal/user_dashboard";
    }


    //Open add form handler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model)
    {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());

        return "normal/add_contact_form";
    }

    // Processing Add Contact form
    @PostMapping("/process-contact")
    public String processContact(@Valid @ModelAttribute Contact contact,
                                 @RequestParam MultipartFile image, // here MultipartFile variable name (i.e; image)
                                 // must be exactly same as form's image input name
                                BindingResult result, Principal principal, HttpSession session)
    {

         try {
            String name = principal.getName();
            User user = userRepos.getUserByUserName(name);

            // Processing and uploading file...
             if (image.isEmpty()) {
                 System.out.println("Image file is empty.");
                 contact.setImgName("contact.png");
             }
             else {
                 System.out.println(image.getOriginalFilename());
                 contact.setImgName(image.getOriginalFilename()); // here we are actually assigning the
                 // image name for the field 'imgName' in the Contact class which will be stored to the Contact table

                 File saveFile = new ClassPathResource("static/img").getFile();
                 Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + image.getOriginalFilename());
                 System.out.println(path);
                 Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                 // Image processing end...
             }

            contact.setUser(user);
            user.getContacts().add(contact);

            userRepos.save(user);
            System.out.println("Data: "+contact);
            System.out.println("Added to database");
            //Sending Success message to alert box...
             session.setAttribute("message",
                     new Message("Your contact is added Successfully!! Add more..", "success"));


         }
         catch (Exception e)
         {
             e.printStackTrace();
             //Sending error message to alert box...
             session.setAttribute("message",
                     new Message("Something went wrong !! Try again..", "danger"));

         }
         return "normal/add_contact_form";
    }

    //show contacts handler
    // contacts per page = 5[n]
    // current page = 0 [page no.]
    @GetMapping("/show_contacts/{page}")
    public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal)
    {
        model.addAttribute("title", "View Contacts");

        // Need to send contact list to the view page
        String userName = principal.getName();
        User user = userRepos.getUserByUserName(userName);

        Pageable pageable = PageRequest.of(page, 7); // Here page => page no.
        Page<Contact> contacts = contactRepos.findContactsByUser(user.getId(), pageable);

        model.addAttribute("contacts", contacts);

        model.addAttribute("currentPageNo", page);
        model.addAttribute("totalPages", contacts.getTotalPages());

        return "normal/show_contacts";
    }

    // To show specific/particular contact details.
    @GetMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId") Integer cId,
                                    Model model, Principal principal)
    {
        System.out.println("CId :"+cId);

        Optional<Contact> optContact = contactRepos.findById(cId);
        Contact contact = optContact.get();

        //* Solving security Bugs
        String userName = principal.getName();
        User loggedInUser = userRepos.getUserByUserName(userName);

        if (loggedInUser.getId() == contact.getUser().getId())
        {
            model.addAttribute("title", contact.getName());

            model.addAttribute("contact", contact);
        }
        //*

        return "normal/contact_detail";
    }

    // delete contact handler
    @GetMapping("/delete/{cId}")
    public String deleteContact(@PathVariable("cId") Integer cId, Model model,
                                Principal principal, HttpSession session) throws IOException {
//        Optional<Contact> optContact = ;
        Contact contact1 = contactRepos.findById(cId).get();

        //*  security check
        String userName = principal.getName();
        User loggedInUser = userRepos.getUserByUserName(userName);

        if (loggedInUser.getId() == contact1.getUser().getId())
        {
            //contact1.setUser(null);
             /*Note: unlink the user from contact
             (use this trick when the handler unable to delete the contact)
             This statement is optional in my case
            */

            // Before deleting the contact, delete the profile photo from the storage
            String imgName = contact1.getImgName();
            if (imgName!=null && !imgName.equals("contact.png"))
            {
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + imgName);
                System.out.println(path);
                Files.delete(path);
            }
            // Image deleted...!!

            contactRepos.delete(contact1);

            session.setAttribute("message",
                    new Message("Contact deleted successfully..!!", "success"));
        }

        return "redirect:/user/show_contacts/0";
    }

    //Open update form handler
    @PostMapping("/update_contact/{cId}")
    public String updateForm(@PathVariable("cId") Integer cId, Model model)
    {

        model.addAttribute("title", "Update Contact");
        Contact contact = contactRepos.findById(cId).get();

        model.addAttribute("contact", contact);
        return "normal/update_form";
    }

    //Update contact handler, Processing Update Contact form
    @PostMapping("/process-update")
    public String processUpdate(@ModelAttribute Contact contact,
                                 @RequestParam MultipartFile image, Model model,
                                 Principal principal, HttpSession session)
    {

        System.out.println("Contact Name: "+contact.getName());
        System.out.println("Contact Id: "+contact.getcId());

        try {

            //**  deleting the old profile photo from the storage

            //* old contact detail
            Contact oldContactDetail = this.contactRepos.findById(contact.getcId()).get();

            String imgName = oldContactDetail.getImgName();
            System.out.println(imgName);

            //* fixing null img name
            if (imgName==null)
            {
                oldContactDetail.setImgName("contact.png");
            }
            //*

            if (!image.isEmpty() && !imgName.equals("contact.png"))
            {
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + imgName);
                Files.delete(path);

                System.out.println(path);
                System.out.println("old image file deleted.");
            }
            //old Image deleted...!!


            // saving new profile image
            if (!image.isEmpty()) {
                contact.setImgName(image.getOriginalFilename());
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + image.getOriginalFilename());
                System.out.println(path);
                Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            }
            else {
                contact.setImgName(oldContactDetail.getImgName());
            }

            User user = userRepos.getUserByUserName(principal.getName());
            contact.setUser(user);

            contactRepos.save(contact);

            System.out.println("Data: "+contact);
            System.out.println("Added to database");
            //Sending Success message to alert box...
            session.setAttribute("message",
                    new Message("Your contact is Updated Successfully!! ", "success"));


        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            //Sending error message to alert box...
            session.setAttribute("message",
                    new Message("Something went wrong !! Try again..", "danger"));
        }

        return "redirect:/user/"+contact.getcId()+"/contact";

    }

    //your profile handler
    @GetMapping("/profile")
    public String yourProfile(Model model)
    {
        model.addAttribute("title", "Profile Page");
        return "normal/profile";
    }

    // Settings handler
    @GetMapping("/settings")
    public String settingsHandler (Model model)
    {
        model.addAttribute("title", "Settings Page");
        return "normal/settings";
    }

    // logged in home
    @GetMapping("/home")
    public String loggedInHome()
    {
        return "normal/loggedin_home";
    }

    // change password handler
    @PostMapping("/change-password")
    public String changePassword( @RequestParam("oldPassword") String oldPassword,
                                  @RequestParam("newPassword") String newPassword,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  Principal principal, HttpSession session)
    {

        System.out.println(oldPassword);
        System.out.println(newPassword);
        System.out.println(confirmPassword);

        String userName = principal.getName();

        User currentUser = userRepos.getUserByUserName(userName);
        System.out.println(currentUser);

        if ( bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()) &&
                !newPassword.equals(oldPassword) && newPassword.equals(confirmPassword) )
        {
            //change the password
            currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
            userRepos.save(currentUser);
            session.setAttribute("message",
                    new Message("Your password has been changed. ", "success"));

        }
        else {
            session.setAttribute("message",
                    new Message("The passwords you entered did not match.", "danger"));
            return "redirect:/user/settings";

        }
        return "redirect:/user/dashboard";
    }

    // Creating order for payment
    @PostMapping("/create_order")
    @ResponseBody
    public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws Exception {
        System.out.println("order function executed.");
        System.out.println(data);
        int amt = Integer.parseInt(data.get("amount").toString());
        System.out.println(amt);

        var client = new RazorpayClient("rzp_test_v3x9FAyRos2UEk","8v6n0c5EMZobNeKSD2wFU9pP");

        JSONObject obj = new JSONObject();
        obj.put("amount", amt*100); // Here we are converting Rupees amount into Paisa
        obj.put("currency","INR");
        obj.put("receipt","txn_2411023");

        //Creating new order
        Order order = client.orders.create(obj);
        System.out.println(order);
        //save the order info in database:
        MyOrder myOrder = new MyOrder();
        myOrder.setAmount(order.get("amount")+""); // convert into integer rupees
        myOrder.setOrderId(order.get("id"));
        myOrder.setPaymentId(null);
        myOrder.setStatus("created");
        myOrder.setUser(userRepos.getUserByUserName(principal.getName()));
        myOrder.setReceipt(order.get("receipt"));

        myOrderRepos.save(myOrder);

        return order.toString();
    }

    //handler for update order
    @PostMapping("/update_order")
    public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data)
    {
        MyOrder myOrder = myOrderRepos.findByOrderId(data.get("order_id").toString());
        myOrder.setPaymentId(data.get("payment_id").toString());
        myOrder.setStatus(data.get("status").toString());

        myOrderRepos.save(myOrder);

        System.out.println(data);
        return ResponseEntity.ok(Map.of("msg", "updated"));
    }

}
