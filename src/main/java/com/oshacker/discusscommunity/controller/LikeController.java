package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.annotation.LoginRequired;
import com.oshacker.discusscommunity.entity.Comment;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Event;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.event.EventProducer;
import com.oshacker.discusscommunity.service.LikeService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.HostHolder;
import com.oshacker.discusscommunity.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements DiscussCommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/like",method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType,int entityId,int entityUserId,int postId) {
        User user=hostHolder.getUser();
        if(user==null) {
            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
        }

        //点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        //点赞数量
        long likeCount=likeService.findEntityLikeCount(entityType,entityId);
        //点赞状态
        int likeStatus=likeService.findEntityLikeStatus(user.getId(),entityType,entityId);
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞（非点踩）事件
        if (likeStatus==1) {
            Event event=new Event().setTopic(TOPIC_LIKE).setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType).setEntityId(entityId).setEntityUserId(entityUserId)
                    .setData("postId",postId);
            eventProducer.fireEvent(event);
        }

        if (entityType==ENTITY_TYPE_POST) {
            //对帖子点赞时，将帖子id放到Redis中，以便计算帖子分数
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(postScoreKey,postId);
        }

        return DiscussCommunityUtil.getJSONString(0,null,map);
    }
}
