package com.sscm.service;


import com.sscm.entities.EmailTemplate;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public boolean sendMail(EmailTemplate emailTemp)
    {

        boolean flag = false;
        try {

            MimeMessage message = javaMailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message);
            MimeMessageHelper helper = new MimeMessageHelper(message, true);// Set the second parameter to true for HTML content

            helper.setFrom("sinsjohnymia321@gmail.com");
            helper.setTo(emailTemp.getTo());
            helper.setSubject(emailTemp.getSubject());
//            helper.setText(emailTemp.getMessage());
            helper.setText(emailTemp.getMessage(), true); // Set the second parameter to true for HTML content

            javaMailSender.send(message);

            flag = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return flag;
    }
}
