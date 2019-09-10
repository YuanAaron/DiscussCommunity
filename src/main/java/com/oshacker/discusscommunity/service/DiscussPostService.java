package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.dao.DiscussPostMapper;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.utils.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    public int findDiscussPostRows(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost discussPost) {
        if (discussPost==null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        //转义HTML标签
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        //敏感词过滤
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
        return discussPostMapper.insertDiscussPost(discussPost)>0?discussPost.getId():0;
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    int updateCommentCount(int id,int commentCount) {
        return discussPostMapper.updateCommentCount(id,commentCount);
    }


}
