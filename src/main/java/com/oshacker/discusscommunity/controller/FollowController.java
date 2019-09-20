package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.Page;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.FollowService;
import com.oshacker.discusscommunity.service.UserService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements DiscussCommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @RequestMapping(path="/followers/{userId}",method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user==null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user",user);

        page.setLimit(5);
        page.setPath("/followers/"+userId);
        page.setRows((int)followService.findFollowerCount(ENTITY_TYPE_USER,userId));

        List<Map<String, Object>> followers = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (followers!=null) {
            for (Map<String,Object> map: followers) {
                User u=(User)map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("followers",followers);
        return "/site/follower";
    }

    @RequestMapping(path="/followees/{userId}",method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user==null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user",user);

        page.setLimit(5);
        page.setPath("/followees/"+userId);
        page.setRows((int)followService.findFolloweeCount(userId,ENTITY_TYPE_USER));

        List<Map<String, Object>> followees = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (followees!=null) {
            for (Map<String,Object> map: followees) {
                User u=(User)map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("followees",followees);
        return "/site/followee";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser()==null)
            return false;
        return followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
    }

    @RequestMapping(path="/unfollow",method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType,int entityId) {
        User user=hostHolder.getUser();
        if (user==null) {
            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
        }

        followService.unfollow(user.getId(),entityType,entityId);
        return DiscussCommunityUtil.getJSONString(0,"已取消关注!");
    }

    @RequestMapping(path="/follow",method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType,int entityId) {
        User user=hostHolder.getUser();
        if (user==null) {
            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
        }

        followService.follow(user.getId(),entityType,entityId);
        return DiscussCommunityUtil.getJSONString(0,"已关注!");
    }
    
}
