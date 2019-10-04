package com.oshacker.discusscommunity.interceptor;

import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.DataService;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataIntercepter implements HandlerInterceptor{

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //将IP记入UV
        String IP = request.getRemoteHost();
        dataService.recordUV(IP);

        //将用户id记入DAU
        User user = hostHolder.getUser();
        if (user!=null) {
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
