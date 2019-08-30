package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
//@Scope("prototype") //不在程序启动时实例化Bean，而是每次getBean都会实例化一个Bean
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    public AlphaService() {
//        System.out.println("实例化AlphaService");
    }

    @PostConstruct //该注解表示在构造函数之后调用
    public void init() {
//        System.out.println("初始化AlphaService");
    }

    @PreDestroy //该注解表示在销毁之前调用
    public void destroy() {
//        System.out.println("销毁AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }

}
