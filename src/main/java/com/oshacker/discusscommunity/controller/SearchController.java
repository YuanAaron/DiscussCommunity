package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Page;
import com.oshacker.discusscommunity.service.ElasticsearchService;
import com.oshacker.discusscommunity.service.LikeService;
import com.oshacker.discusscommunity.service.UserService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements DiscussCommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    //GET请求：参数传递/search?keyword=xxx 或 /search/{keyword}
    @RequestMapping(path="/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        org.springframework.data.domain.Page<DiscussPost> searchResult = elasticsearchService
                .searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if (searchResult!=null) {
            for (DiscussPost post: searchResult) {
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                map.put("user",userService.findUserById(post.getUserId()));
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword); //将keyword传给模板，设置到搜索框

        //设置分页
        page.setPath("/search?keyword="+keyword);
        page.setRows(searchResult==null?0: (int) searchResult.getTotalElements());

        return "/site/search";
    }
}
