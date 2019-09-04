package com.oshacker.discusscommunity.utils;

public interface DiscussCommunityConstant {

    /**
     * 激活成功
     */
    int ACTIVATION_SUCCESS = 0;

    /**
     * 重复激活
     */
    int ACTIVATION_REPEAT = 1;

    /**
     * 激活失败
     */
    int ACTIVATION_FAILURE = 2;

    /**
     * 默认状态下的登录凭证超时时间
     */
    int DEFAULT_EXPIRED_SECONDS = 3600*12; //12h

    /**
     * 记住我状态下的登录凭证超时时间
     */
    int REMEMBERME_EXPIRED_SECONDS = 3600*24*100; //100d

}
