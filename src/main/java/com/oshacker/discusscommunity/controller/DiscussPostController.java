package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

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

        //报错的情况将来统一处理
        return DiscussCommunityUtil.getJSONString(0,"发布成功");
    }

}
