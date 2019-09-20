package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.dao.CommentMapper;
import com.oshacker.discusscommunity.dao.DiscussPostMapper;
import com.oshacker.discusscommunity.entity.Comment;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements DiscussCommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    //涉及到事务管理：添加评论的DML和修改discuss_post中评论数的DML
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment==null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        //添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        //更新discuss_post表中的评论数量
        if (comment.getEntityType()==ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(ENTITY_TYPE_POST, comment.getEntityId());
            discussPostMapper.updateCommentCount(comment.getEntityId(),count);
        }

        return rows;
    }

    public List<Comment> findUserComments(int userId, int offset, int limit) {
        return commentMapper.selectCommentsByUser(userId, offset, limit);
    }

    public int findUserCount(int userId) {
        return commentMapper.selectCountByUser(userId);
    }

}
