package com.oshacker.discusscommunity.utils;

import com.oshacker.discusscommunity.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息,用于代替session对象（它是线程隔离的，但程序中不喜欢用）.
 */
@Component
public class HostHolder {

    //线程隔离，每个线程单独存一份
    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }

}
