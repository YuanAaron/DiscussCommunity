package com.oshacker.discusscommunity.dao;

import com.oshacker.discusscommunity.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //userId==0表示所有帖子，userId!=0表示某个用户发布的帖子
    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit,
                                         @Param("orderMode") int orderMode);

    int selectDiscussPostRows(int userId);

    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(@Param("id") int id,
                           @Param("commentCount") int commentCount);

    int updateType(@Param("id") int id,@Param("type") int type);

    int updateStatus(@Param("id") int id,@Param("status") int status);

    int updateScore(@Param("id") int id,@Param("score") double score);

}
