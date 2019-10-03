package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.*;
import com.oshacker.discusscommunity.event.EventProducer;
import com.oshacker.discusscommunity.service.CommentService;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.service.LikeService;
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

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements DiscussCommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    //删帖
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        User user = hostHolder.getUser();
        //因为使用了Spring Security进行权限控制，这里可以省略
//        if (user==null) {
//            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
//        }

        discussPostService.updateStatus(id,2);

        //触发删帖事件(同步到elasticsearch中)
        Event event=new Event().setTopic(TOPIC_DELETE).setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return DiscussCommunityUtil.getJSONString(0);
    }

    //加精
    @RequestMapping(path = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        User user = hostHolder.getUser();
        //因为使用了Spring Security进行权限控制，这里可以省略
//        if (user==null) {
//            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
//        }

        discussPostService.updateStatus(id,1);

        //触发发帖事件(同步到elasticsearch中)
        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return DiscussCommunityUtil.getJSONString(0);
    }

    //置顶
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        User user = hostHolder.getUser();
        //因为使用了Spring Security进行权限控制，这里可以省略
//        if (user==null) {
//            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
//        }

        discussPostService.updateType(id,1);

        //触发发帖事件(同步到elasticsearch中)
        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return DiscussCommunityUtil.getJSONString(0);
    }

    @RequestMapping(path="/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int id, Model model, Page page) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        model.addAttribute("post",post);

        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        // 关于点赞
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, id);
        model.addAttribute("likeCount", likeCount);
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, id);
        model.addAttribute("likeStatus", likeStatus);

        //评论相关：本项目中给帖子的评论叫评论，给评论的评论叫回复。

        //评论分页信息
        page.setLimit(5);
        page.setRows(post.getCommentCount()); //评论数量
        page.setPath("/discuss/detail/"+id);

        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST,
                post.getId(), page.getOffset(), page.getLimit());
        //评论VO列表
        List<Map<String,Object>> commentVoList=new ArrayList<>();
        if (commentVoList!=null) {
            for (Comment comment: commentList) {
                //评论VO
                Map<String,Object> commentVo=new HashMap<>();
                commentVo.put("comment",comment);
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                // 关于点赞
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus",likeStatus);

                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT,
                        comment.getId(), 0, Integer.MAX_VALUE);
                //回复VO列表
                List<Map<String,Object>> replyVoList=new ArrayList<>();
                if (replyVoList!=null) {
                    for (Comment reply: replyList) {
                        //回复VO
                        Map<String,Object> replyVo=new HashMap<>();
                        replyVo.put("reply",reply);
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        replyVo.put("target",reply.getTargetId()==0?null:userService.findUserById(reply.getTargetId()));
                        // 关于点赞
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount);
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus",likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys",replyVoList);
                //回复数量
                commentVo.put("replyCount",commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId()));

                commentVoList.add(commentVo);
            }
            model.addAttribute("comments",commentVoList);
        }

        return "/site/discuss-detail";
    }

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content) {
        User user = hostHolder.getUser();
        if (user==null) {
            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
        }

        DiscussPost post=new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //触发发帖事件
        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(post.getId());
        eventProducer.fireEvent(event);

        //报错的情况将来统一处理
        return DiscussCommunityUtil.getJSONString(0,"发布成功");
    }

}
