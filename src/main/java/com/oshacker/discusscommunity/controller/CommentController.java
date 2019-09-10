package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.Comment;
import com.oshacker.discusscommunity.service.CommentService;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

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
        return "redirect:/discuss/detail/" + discussPostId;
    }
}
