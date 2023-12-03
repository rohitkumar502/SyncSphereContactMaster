package com.sscm.controller;

import com.sscm.dao.ContactRepository;
import com.sscm.dao.UserRepository;
import com.sscm.entities.Contact;
import com.sscm.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class SearchController {

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private ContactRepository contactRepos;

    // Search handler
    @GetMapping("/search/{query}")
    public ResponseEntity<?> search(@PathVariable("query") String query, Principal principal)
    {
//        System.out.println(query);
        User user = userRepos.getUserByUserName(principal.getName());
        List<Contact> contacts = contactRepos.findByNameContainingAndUser(query, user);
        return ResponseEntity.ok(contacts);
    }

}
