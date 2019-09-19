package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.annotation.LoginRequired;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.LikeService;
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
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/like",method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType,int entityId) {
        User user=hostHolder.getUser();
        if(user==null) {
            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
        }

        //点赞
        likeService.like(user.getId(),entityType,entityId);
        //点赞数量
        long likeCount=likeService.findEntityLikeCount(entityType,entityId);
        //点赞状态
        int likeStatus=likeService.findEntityLikeStatus(user.getId(),entityType,entityId);
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);
        return DiscussCommunityUtil.getJSONString(0,null,map);
    }
}
