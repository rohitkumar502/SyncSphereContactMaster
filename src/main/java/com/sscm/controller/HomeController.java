package com.sscm.controller;

import com.sscm.dao.UserRepository;
import com.sscm.entities.User;
import com.sscm.helper.Message;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository repos;

    @GetMapping("/")
    public String home(Model m)
    {
        m.addAttribute("title", "Home - SyncSphereContactMaster");
        return "home";
    }

    @GetMapping("/about")
    public String about(Model m)
    {
        m.addAttribute("title", "About - Smart Contact Manager");
        return "about";
    }

    @GetMapping("/signup")
    public String signup(Model m)
    {
        m.addAttribute("title", "SignUp - Smart Contact Manager");
        m.addAttribute("user", new User());
        return "signup";
    }

    //this handler for registering user
    @PostMapping("/do_register")
    public String registerUser(@Valid @ModelAttribute("user") User user1, BindingResult result,
                               @RequestParam(value="agreement", defaultValue="false") boolean agreement,
                               Model model, HttpSession session)
    {
        try {
            if (!agreement)
            {
               // System.out.println("You have not agreed terms and conditions.");
                throw new Exception("You have not agreed terms and conditions.");
            }

            if (result.hasErrors())
            {
                System.out.println("ERROR "+result.toString());
                model.addAttribute("user", user1);
                return "signup";
            }

            user1.setRole("ROLE_USER");
            user1.setEnabled(true);
            user1.setImgUrl("default.png");
            user1.setPassword(passwordEncoder.encode(user1.getPassword()));

            User res = repos.save(user1);

//            System.out.println("Agreement "+agreement);
//            System.out.println("User "+user1);
            model.addAttribute("user", new User());

            session.setAttribute("message",
                    new Message("Successfully Registered !!", "alert-success"));
            return "signup";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            model.addAttribute("user", user1);
            session.setAttribute("message",
                    new Message("Something went wrong !! "+ e.getMessage(), "alert-danger"));
            return "signup";
        }

    }


    //handler for custom login
    @GetMapping("/signin")
    public String customLogin(Model model)
    {
        model.addAttribute("title", "Login Page");
        return "login";
    }



}
