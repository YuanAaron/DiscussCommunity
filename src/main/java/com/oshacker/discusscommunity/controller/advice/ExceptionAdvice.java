package com.oshacker.discusscommunity.controller.advice;

import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//当带有@Controller的Bean发生异常，记录日志
@ControllerAdvice(annotations = {Controller.class})
public class ExceptionAdvice {

    private static final Logger logger= LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        //记录异常的概括信息
        logger.error("服务器发生异常: "+e.getMessage());
        //记录异常时详细的栈信息
        for (StackTraceElement element: e.getStackTrace()) {
            logger.error(element.toString());
        }

        String xRequestedWith=request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) { //异步请求：浏览器访问服务器时，服务器返回JSON数据。
            response.setContentType("application/plain;charset=utf-8");
            response.getWriter().write(DiscussCommunityUtil.getJSONString(1,"服务器异常!"));
        } else { //普通请求：浏览器访问服务器时，服务器返回页面。
            response.sendRedirect(request.getContextPath()+"/error");
        }
    }

}
