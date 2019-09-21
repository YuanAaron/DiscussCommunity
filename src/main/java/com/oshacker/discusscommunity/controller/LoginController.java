package com.oshacker.discusscommunity.controller;

import com.google.code.kaptcha.Producer;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.UserService;
import com.oshacker.discusscommunity.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements DiscussCommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //重置密码
    @RequestMapping(path = {"/forget/password"},method = {RequestMethod.POST})
    public String resetPassword(String email,String verifyCode,String newPassword,
                                Model model,HttpSession session) {
        //先检查验证码
        String code = (String) session.getAttribute("verifyCode");
        if (StringUtils.isBlank(code) || StringUtils.isBlank(verifyCode) || !code.equalsIgnoreCase(verifyCode)) {
            model.addAttribute("codeMsg","验证码不正确!");
            return "/site/forget";
        }

        Map<String, Object> map = userService.resetPassword(email, newPassword);
        if (map.containsKey("user")) {
            return "redirect:/login";
        }
        model.addAttribute("emailMsg",map.get("emailMsg"));
        model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
        return "/site/forget";
    }

    @RequestMapping(path = {"/forget/code"},method = {RequestMethod.GET})
    @ResponseBody
    public String getForgetCode(String email,HttpSession session) {
        if (StringUtils.isBlank(email)) {
            return DiscussCommunityUtil.getJSONString(1,"邮箱不能为空!");
        }

        Context context=new Context();
        context.setVariable("email",email);
        String code=DiscussCommunityUtil.generateUUID().substring(0,4);
        context.setVariable("verifyCode",code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendEmail(email,"找回密码",content);

        session.setAttribute("verifyCode",code);

        return DiscussCommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = {"/forget"},method= RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    @RequestMapping(path = {"/logout"},method= RequestMethod.GET)
    public String login(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login"; //默认重定向到GET请求的/login
    }

    //对于普通参数，spring MVC不会将其加入到model中，而对于类似User对象则会。
    //方法一：手动将这些普通参数加入model中；
    //方法二：从request对象中通过request.getParameter()获取
    @RequestMapping(path = {"/login"},method= RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model,HttpServletResponse response,
                        HttpServletRequest request) {
        //先检查验证码
        String kaptchaOwner= CookieUtil.getValue(request,"kaptchaOwner");
        String kaptcha=null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String kaptchakey = RedisKeyUtil.getKaptchakey(kaptchaOwner);
            kaptcha=(String)redisTemplate.opsForValue().get(kaptchakey);

        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg","验证码不正确!");
            return "/site/login";
        }

        //检查账号、密码
        int expiredSeconds=rememberme?REMEMBERME_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            //向浏览器下发ticket
            Cookie cookie=new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }

    //生成验证码
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入Redis用于登录
        //验证码的归属: kaptcharOwner是用户获取login.html页面时随机生成的，60s后就过期。
        String kaptchaOwner = DiscussCommunityUtil.generateUUID();
        Cookie cookie=new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        String kaptchakey = RedisKeyUtil.getKaptchakey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchakey,text,60, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    @RequestMapping(path = {"/login"},method= RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    // http://localhost:8080/community/activation/id/code
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model,
                             @PathVariable("userId") int userId,
                             @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = {"/register"},method= RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map==null || map.isEmpty()) { //map==null???
            model.addAttribute("msg","注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }

        model.addAttribute("usernameMsg",map.get("usernameMsg"));
        model.addAttribute("passwordMsg",map.get("passwordMsg"));
        model.addAttribute("emailMsg",map.get("emailMsg"));
        return "/site/register";

    }

    @RequestMapping(path = {"/register"},method= RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }
}
