package com.oshacker.discusscommunity.dao;

import org.springframework.stereotype.Repository;

@Repository("alphaHibernate") //Bean的默认名字为alphaHibernateImpl，这里自定义了名字
public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "Hibernate";
    }
}
