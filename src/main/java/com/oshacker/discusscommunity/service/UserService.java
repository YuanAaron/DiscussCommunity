package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.dao.LoginTicketMapper;
import com.oshacker.discusscommunity.dao.UserMapper;
import com.oshacker.discusscommunity.entity.LoginTicket;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.MailClient;
import com.oshacker.discusscommunity.utils.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements DiscussCommunityConstant {

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

    @Autowired
    private RedisTemplate redisTemplate;

    //获取某个用户的权限
    //关键：在什么时候获得用户的权限，并将与用户权限相关的Token存到SecurityContext中？
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = userMapper.selectById(userId);
        if (user==null) {
            throw new RuntimeException("该用户不存在!");
        }

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

    //关于缓存
    //1、先不去访问MySQL,而是优先从缓存中取值
    private User getCache(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    //2、如果从缓存中取到了就直接用，如果取不到就初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    //3、用户数据变更时（比如修改头像，密码等）更新缓存（具体做法是清除缓存数据）
    private void clearCache(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword) {
        Map<String,Object> map=new HashMap<>();
        
        // 空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空!");
            return map;
        }
        
        //验证原始密码
        User user = userMapper.selectById(userId);
        oldPassword=DiscussCommunityUtil.md5(oldPassword+user.getSalt());
        if (!oldPassword.equals(user.getPassword())) {
            map.put("oldPasswordMsg","原密码不正确!");
            return map;
        }

        newPassword=DiscussCommunityUtil.md5(newPassword+user.getSalt());
        userMapper.updatePassword(userId,newPassword);
        clearCache(userId);
        return map;
    }

    public Map<String,Object> resetPassword(String email,String newPassword) {
        Map<String,Object> map=new HashMap<>();
        
        //空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg","邮箱不能为空!");
            return map;
        }

        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg","新密码不能为空!");
            return map;
        }

        //验证邮箱
        User user = userMapper.selectByEmail(email);
        if (user==null) {
            map.put("emailMsg","该邮箱尚未注册!");
            return map;
        }

        //重置密码
        newPassword=DiscussCommunityUtil.md5(newPassword+user.getSalt());
        userMapper.updatePassword(user.getId(),newPassword);
        clearCache(user.getId());

        map.put("user",user);
        return map;
    }

    public int updateHeader(int userId, String headerUrl) {
        int rows=userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public LoginTicket findLoginTicket(String ticket) {
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    public void logout(String ticket) {
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }

    public Map<String,Object> login(String username,String password,long expiredSeconds) {
        Map<String,Object> map=new HashMap<>();

        //空值判断
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg","账号不能为空!");
            return map;
        }

        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg","密码不能为空!");
            return map;
        }

        //检验账号是否存在
        User user = userMapper.selectByName(username);
        if (user==null) {
            map.put("usernameMsg","该账号不存在!");
            return map;
        }

        //检验账号是否激活
        if (user.getStatus()==0) {
            map.put("usernameMsg","该账号未激活!");
            return map;
        }

        //用户名存在且已激活，检验密码是否正确
        password=DiscussCommunityUtil.md5(password+user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg","密码不正确!");
            return map;
        }

        //用户名存在且已激活，密码正确，即登录成功

        //登录成功后生成登录凭证
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000));
        loginTicket.setStatus(0);
        loginTicket.setTicket(DiscussCommunityUtil.generateUUID());

        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey,loginTicket);

        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

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
        return map;
    }

    public User findUserById(int id) {
        User user = getCache(id);
        if (user==null) {
            user = initCache(id);
        }
        return user;
    }
}
