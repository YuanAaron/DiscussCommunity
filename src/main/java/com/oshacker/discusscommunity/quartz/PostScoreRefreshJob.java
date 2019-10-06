package com.oshacker.discusscommunity.quartz;

import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.service.ElasticsearchService;
import com.oshacker.discusscommunity.service.LikeService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, DiscussCommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    //牛客纪元
    private static final Date epoch;

    static {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            epoch = sdf.parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败!",e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(postScoreKey);

        if (operations.size()==0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: "+operations.size());
        while (operations.size()>0) {
            postScoreRefresh((Integer)operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    private void postScoreRefresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post==null) {
            logger.error("该帖子不存在 id = "+postId);
            return;
        }

        if (post.getStatus()==2) {
            logger.error("该帖子已被删除");
            return;
        }

        //利用公式计算帖子分数
        boolean wonderful=post.getStatus()==1; //是否加精
        int commentCount=post.getCommentCount(); //帖子评论数
        long likeCount=likeService.findEntityLikeCount(ENTITY_TYPE_POST,postId); //帖子点赞数

        double w=(wonderful?75:0)+commentCount*10+likeCount*2; //计算权重
        //分数=帖子权重+距离天数
        double score=Math.log10(Math.max(w,1))
                + (post.getCreateTime().getTime()-epoch.getTime())/(1000*3600*20);

        //更新帖子分数
        discussPostService.updateScore(postId,score);

        //同步搜索数据
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);

    }
}
