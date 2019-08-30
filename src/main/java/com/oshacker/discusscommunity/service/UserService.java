package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.dao.UserMapper;
import com.oshacker.discusscommunity.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

}
