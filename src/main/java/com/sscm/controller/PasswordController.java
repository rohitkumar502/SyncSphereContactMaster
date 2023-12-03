package com.sscm.controller;


import com.sscm.dao.UserRepository;
import com.sscm.entities.EmailTemplate;
import com.sscm.entities.User;
import com.sscm.helper.Message;
import com.sscm.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Random;

@Controller
public class PasswordController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    // change password handler
    @PostMapping("/user/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
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

        if ( passwordEncoder.matches(oldPassword, currentUser.getPassword()) &&
                !newPassword.equals(oldPassword) && newPassword.equals(confirmPassword) )
        {
            //change the password
            currentUser.setPassword(passwordEncoder.encode(newPassword));
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



    //email id form open handler
    @GetMapping("/forgot")
    public String openEmailForm()
    {
        return "forgot_email_form";
    }

    // handler for sending otp through email
    @PostMapping("/send-otp")
    public String sendOTP(@ModelAttribute EmailTemplate emailTemp, Model model, HttpSession session)
    {
        model.addAttribute("title", "Email OTP");
        System.out.println("Email To: "+emailTemp.getTo());

        Random random = new Random();
        int otp = random.nextInt(900000) + 100000;
        System.out.println("OTP: " + otp);

        //writing code for send otp to the email
        emailTemp.setSubject("OTP for resetting your SyncSphereContactsMaster password");

        //Method 1:
        emailTemp.setMessage( "Dear,<br>" +"The OTP to reset your SCM account password is <b>" + otp +
                "</b>. Valid for the next 2 hours only.<br><br> Not you? Please report to admin@sscm.com");

        boolean flag = emailService.sendMail(emailTemp);

        if (flag)
        {
            session.setAttribute("DBotp", otp);
            session.setAttribute("email", emailTemp.getTo());
            session.setAttribute("message",new Message("We have sent OTP to your email.", "success"));
            return "verify_otp";
        }
        else {
            session.setAttribute("message",
                    new Message("Something went wrong. Please enter correct email.", "danger"));

            return "redirect:/forgot";
        }

    }

    // Verify otp
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") int otp, Model model, HttpSession session)
    {

        int dbOtp = (int)session.getAttribute("DBotp");
        String email = (String) session.getAttribute("email");

        if (dbOtp == otp)
        {

            User user = userRepos.getUserByUserName(email);
            if (user==null)
            {
                //send error message
                session.setAttribute("message",
                        new Message("User does not exists with this email!!", "danger"));
                return "forgot_email_form";
            }
            else {
                // provide change password form
                model.addAttribute("title", "Reset Password");
                return "reset_password";
            }
        }
        else {
            session.setAttribute("message",
                    new Message("Please enter correct OTP.", "danger"));

            return "verify_otp";
        }

    }

    // Reset Password
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpSession session)
    {

        if (newPassword.equals(confirmPassword))
        {
            String email = (String) session.getAttribute("email");
            User user = userRepos.getUserByUserName(email);

            //change the password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepos.save(user);

            session.setAttribute("message",
                    new Message("Your password has been changed. ", "success"));

        }
        else {
            session.setAttribute("message",
                    new Message("The passwords you entered did not match.", "danger"));
            return "reset_password";
        }

        return "redirect:/signin";
    }


}
