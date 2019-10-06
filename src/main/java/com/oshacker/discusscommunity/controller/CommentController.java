package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.Comment;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Event;
import com.oshacker.discusscommunity.event.EventProducer;
import com.oshacker.discusscommunity.service.CommentService;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.HostHolder;
import com.oshacker.discusscommunity.utils.RedisKeyUtil;
import org.apache.kafka.common.internals.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements DiscussCommunityConstant {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        //隐含传入entityType、entityId以及content（也可能包含targetId）

        //老师没写，后期统一做权限验证
        if (hostHolder.getUser()==null)
            return "redirect:/login";

        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论事件
        Event event=new Event().setTopic(TOPIC_COMMENT).setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType()).setEntityId(comment.getEntityId())
                .setData("postId",discussPostId);
        if (comment.getEntityType()==ENTITY_TYPE_POST) {
            DiscussPost discussPost = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(discussPost.getUserId());
        } else if (comment.getEntityType()==ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        //触发发帖事件（修改帖子评论数）
        if (comment.getEntityType()==ENTITY_TYPE_POST) {
            event=new Event().setTopic(TOPIC_PUBLISH).setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST).setEntityId(discussPostId);
            eventProducer.fireEvent(event);

            //评论帖子时将帖子id放到Redis中，以便计算帖子分数
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(postScoreKey,discussPostId);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
