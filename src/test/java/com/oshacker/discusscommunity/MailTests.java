package com.oshacker.discusscommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import com.oshacker.discusscommunity.utils.MailClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DiscussCommunityApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendEmail("1500438364@qq.com","hello","welcome");
    }

    @Test
    public void testHtmlMail() {
        Context context=new Context();
        context.setVariable("username","zhangsan");
        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);
        mailClient.sendEmail("1500438364@qq.com","HTML",content);

    }



}
