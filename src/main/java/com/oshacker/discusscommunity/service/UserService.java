package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.dao.UserMapper;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${discusscommunity.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private MailClient mailClient;

    public Map<String,Object> register(User user) {
        Map<String,Object> map=new HashMap<>();
        if (user==null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg","用户名不能为空");
            return map;
        }

        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        //验证账号
        //1、用户名不能重复
        User u = userMapper.selectByName(user.getUsername());
        if (u!=null) {
            map.put("usernameMsg","该账号已存在!");
            return map;
        }

        //2、邮箱不能重复
        u = userMapper.selectByEmail(user.getEmail());
        if (u!=null) {
            map.put("emailMsg","该邮箱已存在!");
            return map;
        }

        //其他用户合法性检测就先不做了，比如用户名必须是邮箱手机号，可以通过正则去判断，否则就不让注册

        //注册
        user.setSalt(DiscussCommunityUtil.generateUUID().substring(0,5));
        user.setPassword(DiscussCommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(DiscussCommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //注册成功，给用户发一封激活邮件
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/activation/id/code
        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendEmail(user.getEmail(),"账号激活",content);

        //注册成功，自动登录，同样后台生成ticket
//        String ticket=addLoginTicket(user.getId());//t票和用户关联
//        map.put("ticket",ticket);
        return map;
    }

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }
}
