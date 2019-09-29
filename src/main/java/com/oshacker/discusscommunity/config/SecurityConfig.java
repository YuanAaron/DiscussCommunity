package com.oshacker.discusscommunity.config;

import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements DiscussCommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 忽略静态资源的访问
        web.ignoring().antMatchers("/resources/**");
    }

    //项目已写好了登录、退出的功能，这里继续采用原来的认证方案，因此要绕过Security的认证。
    //正常情况下，Security框架最终将认证的信息封装到UsernamePasswordAuthenticationToken中，
    //这个Token会被Security的filter获取到并存到SecurityContext中。后面授权时通过从SecurityContext中获取Token,
    //从而判断你有没有权限，这是Security底层的逻辑。

    //但是这里我们用的是自己的认证方案，而没有用Security框架，也就没有UsernamePasswordAuthenticationToken，
    //因此Security框架无法帮你做授权，因为它不知道你的权限是什么。因此我们需要自己将认证的结论存储到SecurityContext中。

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权配置
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/user/updatePassword",
                        "/discuss/add",
                        "/comment/add/**",
                        "/like",
                        "/letter/**",
                        "/notice/**",
                        "/follow",
                        "/unfollow"
                ).hasAnyAuthority(AUTHORITY_USER,AUTHORITY_MODERATOR,AUTHORITY_ADMIN)
                //不登录可以访问
                .anyRequest().permitAll()
                //禁用防止csrf攻击的检查
                .and().csrf().disable();

        //权限不够时的处理
        http.exceptionHandling()
                //没有登录
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith=request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) { //异步请求：浏览器访问服务器时，服务器返回JSON数据。
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(DiscussCommunityUtil.getJSONString(403,"你还没有登录哦!"));
                        } else { //普通/同步请求：浏览器访问服务器时，服务器返回页面。
                            response.sendRedirect(request.getContextPath()+"/login");
                        }
                    }
                })
                //权限不足
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith=request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) { //异步请求：浏览器访问服务器时，服务器返回JSON数据。
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(DiscussCommunityUtil.getJSONString(1,"你没有访问此功能的权限!"));
                        } else { //普通/同步请求：浏览器访问服务器时，服务器返回页面。
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }
                });

        //Security底层默认会拦截/logout请求，进行退出处理
        //覆盖默认的退出逻辑（善意的欺骗，其实程序中没有/securitylogout），这样才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout");
    }
}
