package com.oshacker.discusscommunity.utils;

public class RedisKeyUtil {

    private static final String SPLIT=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_USER_LIKE="like:user"; //我收到的赞

    //某个实体的赞：like:entity:entityType:entityId
    public static String getEntityLikeKey(int entityType,int entityId) {
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+entityId;
    }

    //我收到的赞
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE+SPLIT+userId;
    }

}
