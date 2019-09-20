package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;

    public void follow(int userId,int entityType,int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                redisTemplate.multi();
                redisTemplate.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                return redisTemplate.exec();
            }
        });
    }

    public void unfollow(int userId,int entityType,int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                redisTemplate.multi();
                redisTemplate.opsForZSet().remove(followeeKey,entityId);
                redisTemplate.opsForZSet().remove(followerKey,userId);
                return redisTemplate.exec();
            }
        });
    }

    //某个用户关注实体的数量
    public long findFolloweeCount(int userId,int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //某个实体的粉丝的数量
    public long findFollowerCount(int entityType,int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    //查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId,int entityType,int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().score(followerKey,userId)!=null;

    }



}
