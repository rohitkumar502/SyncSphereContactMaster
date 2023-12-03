package com.sscm.controller;

import com.sscm.dao.ContactRepository;
import com.sscm.dao.UserRepository;
import com.sscm.entities.Contact;
import com.sscm.entities.User;
import com.sscm.helper.Message;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private ContactRepository contactRepos;


    @GetMapping("/about")
    public String about(Model m)
    {
        m.addAttribute("title", "About - Smart Contact Manager");
        return "normal/nabout";
    }

    // this method will run every time for every handler to add user data for sending to view
    @ModelAttribute
    public void addCommonData(Model model, Principal principal)
    {
        String userName = principal.getName();

        User user = userRepos.getUserByUserName(userName);

        model.addAttribute("user",user);
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal)
    {
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
                                 @RequestParam MultipartFile image,
                                BindingResult result, Principal principal, HttpSession session)
    {

         try {
            String name = principal.getName();
            User user = userRepos.getUserByUserName(name);

            // Processing and uploading file...
             if (image.isEmpty()) {
                 contact.setImgName("contact.png");
             }
             else {


                 Random random = new Random();
                 int unique = random.nextInt(9000) + 1000;
                 contact.setImgName(image.getOriginalFilename());
                 File saveFile = new ClassPathResource("static/img").getFile();
                 Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + image.getOriginalFilename());
                 System.out.println(path);
                 Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

             }

            contact.setUser(user);
            user.getContacts().add(contact);

            userRepos.save(user);
            System.out.println("Data: "+contact);
//            System.out.println("Added to database");

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


        return "normal/contact_detail";
    }

    // delete contact handler
    @GetMapping("/delete/{cId}")
    public String deleteContact(@PathVariable("cId") Integer cId, Model model,
                                Principal principal, HttpSession session) throws IOException {
        Contact contact1 = contactRepos.findById(cId).get();

        //*  security check
        String userName = principal.getName();
        User loggedInUser = userRepos.getUserByUserName(userName);

        if (loggedInUser.getId() == contact1.getUser().getId())
        {
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

        try {
               Contact oldContactDetail =  contactRepos.findById(contact.getcId()).get();
            if (!image.isEmpty())
            {

               // delete old photo
                if (!Objects.equals(oldContactDetail.getImgName(), "contact.png")) {
                    File deleteFile = new ClassPathResource("static/img").getFile();
                    File file1 = new File(deleteFile, oldContactDetail.getImgName());
                    file1.delete();
                }

                // update new photo
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + image.getOriginalFilename());
                Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                contact.setImgName(image.getOriginalFilename());

            }
            else {
                contact.setImgName(oldContactDetail.getImgName());
            }

            User user = userRepos.getUserByUserName(principal.getName());
            contact.setUser(user);

            contactRepos.save(contact);

            session.setAttribute("message",
                    new Message("Your contact is Updated Successfully!! ", "success"));

        }
        catch (Exception e)
        {
            e.printStackTrace();

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




}
