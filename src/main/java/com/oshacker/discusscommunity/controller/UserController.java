package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.annotation.LoginRequired;
import com.oshacker.discusscommunity.entity.Comment;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Page;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.*;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements DiscussCommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${discusscommunity.path.domain}")
    private String domain;

    @Value("${discusscommunity.path.upload}")
    private String uploadPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private FollowService followService;
    
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @RequestMapping(path="/myreply/{userId}",method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId, Page page,Model model) {
        User user = userService.findUserById(userId);
        if (user==null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user",user);

        //分页相关
        page.setPath("/user/myreply/"+userId);
        page.setRows(commentService.findUserCount(userId));

        List<Comment> commentList = commentService.findUserComments(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("discussPost", post);
                commentVoList.add(map);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/my-reply";
    }

    @RequestMapping(path="/mypost/{userId}",method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId, Page page,Model model) {
        User user = userService.findUserById(userId);
        if (user==null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user",user);

        //分页相关
        page.setPath("/user/mypost/"+userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));

        List<DiscussPost> discussList = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> discussVoList = new ArrayList<>();
        if (discussList != null) {
            for (DiscussPost post : discussList) {
                Map<String, Object> map = new HashMap<>();
                map.put("discussPost", post);
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussVoList.add(map);
            }
        }
        model.addAttribute("discussPosts", discussVoList);
        return "/site/my-post";
    }

    @RequestMapping(path="/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model) {
        User user = userService.findUserById(userId);
        if (user==null) {
            throw new RuntimeException("该用户不存在!");
        }
        
        model.addAttribute("user",user);
        //用户收到的赞
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);

        //用户关注的数量
        model.addAttribute("followeeCount",followService.findFolloweeCount(userId, ENTITY_TYPE_USER));
        //用户的粉丝数量
        model.addAttribute("followerCount",followService.findFollowerCount(ENTITY_TYPE_USER,userId));
        //当前登录用户是否已关注该用户
        boolean hasFollowed=false;
        if (hostHolder.getUser()!=null) {
            hasFollowed=followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }

    @RequestMapping(path = {"/updatePassword"}, method = RequestMethod.POST)
    public String updatePassword(String oldPassword,String newPassword,Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map==null || map.isEmpty()) {
            return "redirect:/logout";
        }

        model.addAttribute("oldPasswordMsg",map.get("oldPasswordMsg"));
        model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
        return "/site/setting";
    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".")+1);
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(fileName);
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    @LoginRequired
    //当传入多张图片时，使用MultipartFile[]
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf(".")); //获得图片格式
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = DiscussCommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 图片存储成功后，更新当前用户头像的web访问路径
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

}
