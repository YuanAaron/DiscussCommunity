package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Page;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.service.LikeService;
import com.oshacker.discusscommunity.service.UserService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements DiscussCommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    //拒绝访问时的提示页面
    @RequestMapping(path="/denied",method = RequestMethod.GET)
    public String getDeniedPage() {
        return "error/404";
    }

    @RequestMapping(path="/error",method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    //for produce environment：老师的做法
//    @RequestMapping(path = "/", method = RequestMethod.GET)
//    public String root() {
//        return "forward:/index";
//    }

    //for produce environment :我的做法
    @RequestMapping(path = {"/","/index"}, method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(value = "orderMode",defaultValue = "0") int orderMode) {
        // 方法调用前，SpringMVC的DispatcherServlet会自动实例化Model和Page,
        // 而且会将Page注入Model.所以,在thymeleaf中可以直接访问Page对象中的数据.
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode="+orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                long likeCount=likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId());
                map.put("likeCount",likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode",orderMode);
        return "/index";
    }

}
