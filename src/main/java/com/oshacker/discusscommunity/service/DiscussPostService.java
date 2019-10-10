package com.oshacker.discusscommunity.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oshacker.discusscommunity.dao.DiscussPostMapper;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.utils.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger= LoggerFactory.getLogger(DiscussPostService.class);

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffine.posts.expire-seconds}")
    private int expireSeconds;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    //Caffeine核心接口是Cache，其子接口有LoadingCache（同步缓存）和AsyncLoadingCache（异步缓存）。
    //LoadingCache: 多个线程同时访问缓存中的同一份数据，如果缓存中没有该数据，它会让所有线程都阻塞，
    //然后从数据库中加载数据到缓存，取到以后再返回，这样所有的线程都可以获取到该数据。
    //AsyncLoadingCache：支持多个线程并发的从数据库中取数据，我们一般希望的是数据加载到缓存以后再返回，不要并发的取。
    //因此我们一般用LoadingCache。
    //二者的区别是，异步不会阻塞当前任务，同步缓存直到同步方法处理完才能继续往下执行。

    //帖子列表的缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init() {
        //初始化帖子列表缓存
        postListCache= Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        //缓存中没有数据，查询数据库，加载到缓存中
                        if (key==null || key.length()==0) {
                            throw new IllegalArgumentException("参数错误");
                        }

                        String[] params = key.split(":");
                        if (params==null || params.length!=2) {
                            throw new IllegalArgumentException("参数错误");
                        }

                        int offset=Integer.parseInt(params[0]);
                        int limit=Integer.parseInt(params[1]);

                        //注意：可以在这里添加二级缓存，即如果本地缓存中没有数据就访问redis,二级缓存中没有才访问DB。

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit,1);
                    }
                });
        //初始化帖子总数缓存
        postRowsCache=Caffeine.newBuilder()
                .maximumSize(maxSize) //这里其实1个就够了,用不了15个
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit,int orderMode) {
        //首页热门帖子才启用缓存
        if (userId==0 && orderMode==1) {
            return postListCache.get(offset+":"+limit);
        }

        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }

    public int findDiscussPostRows(int userId) {
        //首页帖子数量才启用缓存
        if (userId==0) {
            return postRowsCache.get(userId);
        }

        logger.debug("load post rows from DB.");
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

    public int updateCommentCount(int id,int commentCount) {
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    public int updateType(int id,int type) {
        return discussPostMapper.updateType(id,type);
    }

    public int updateStatus(int id,int status) {
        return discussPostMapper.updateStatus(id,status);
    }

    public int updateScore(int id,double score) {
        return discussPostMapper.updateScore(id,score);
    }

}
