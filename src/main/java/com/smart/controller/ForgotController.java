package com.smart.controller;


import com.smart.dao.UserRepository;
import com.smart.entities.EmailTemplate;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Random;

@Controller
public class ForgotController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    //email id form open handler

    @GetMapping("/forgot")
    public String openEmailForm()
    {
        return "forgot_email_form";
    }

    // handler for sending otp through email
    @PostMapping("/send-otp")
//    public String sendOTP(@RequestParam("email") String email, HttpSession session)
    public String sendOTP(@ModelAttribute EmailTemplate emailTemp, Model model, HttpSession session)
    {
        model.addAttribute("title", "Email OTP");
        System.out.println("Email To: "+emailTemp.getTo());

        Random random = new Random();
        // Generate a random 4-digit number
        // generating otp of 4 digits
//        int otp = random.nextInt(9000) + 1000;
        int otp = random.nextInt(900000) + 100000;
        System.out.println("OTP: " + otp);

        //writing code for send otp to the email
        emailTemp.setSubject("OTP for resetting your SCM password");

        //Method 1:
        emailTemp.setMessage( "Dear,<br>" +"The OTP to reset your SCM account password is <b>" + otp +
                "</b>. Valid for the next 2 hours only.<br><br> Not you? Please report to admin@scm.com");

        boolean flag = emailService.sendMail(emailTemp);

        if (flag)
        {
            session.setAttribute("DBotp", otp);
            session.setAttribute("email", emailTemp.getTo());
//            session.setAttribute("message",new Message("Email send Successfully", "success"));
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

        System.out.println("dbOtp: "+dbOtp);
        System.out.println("Otp: "+otp);
        System.out.println("email: "+email);

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

        System.out.println("newPassword: "+newPassword);
        System.out.println("confirmPassword: "+confirmPassword);

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

            System.out.println("passwords didn't same.");
            session.setAttribute("message",
                    new Message("The passwords you entered did not match.", "danger"));
//            return "redirect:/reset-password";
            return "reset_password";
        }

        return "redirect:/signin";
    }


}
