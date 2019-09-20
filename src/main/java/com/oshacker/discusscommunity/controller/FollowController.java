package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.FollowService;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

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
}
