package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.dao.MessageMapper;
import com.oshacker.discusscommunity.entity.Message;
import com.oshacker.discusscommunity.entity.Page;
import com.oshacker.discusscommunity.entity.User;
import com.oshacker.discusscommunity.service.MessageService;
import com.oshacker.discusscommunity.service.UserService;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.oshacker.discusscommunity.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @RequestMapping(path="/letter/delete",method = RequestMethod.POST)
    @ResponseBody
    public String deleteLetter(int id) {
        messageService.deleteMessage(id);
        return DiscussCommunityUtil.getJSONString(0);
    }

    @RequestMapping(path="/letter/send",method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName,String content) {
        User user = hostHolder.getUser();
        if (user==null) {
            return DiscussCommunityUtil.getJSONString(403,"你还未登录哦!");
        }
        User target = userService.findUserByName(toName);
        if (target==null) {
            return DiscussCommunityUtil.getJSONString(1,"目标用户不存在!");
        }

        Message message=new Message();
        message.setFromId(user.getId());
        message.setToId(target.getId());
        if (message.getFromId()<message.getToId()) {
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        } else {
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        //报错的情况将来统一处理
        return DiscussCommunityUtil.getJSONString(0);
    }

    @RequestMapping(path="/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId,
                                  Model model,Page page) {
        //设置分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        //私信列表
        List<Message> letterList = messageService.findLetters(
                conversationId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> letters=new ArrayList<>();
        if (letterList!=null) {
            for (Message message:letterList) {
                Map<String,Object> map=new HashMap<>();
                map.put("letter",message);
                map.put("fromUser",userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters",letters);
        model.addAttribute("target",getLetterTarget(conversationId));

        //设置已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "/site/letter-detail";
    }

    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids=new ArrayList<>();
        if (letterList!=null) {
            for (Message message: letterList) {
                if (hostHolder.getUser().getId()==message.getToId()&&message.getStatus()==0)
                    ids.add(message.getId());
            }
        }
        return ids;
    }

    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0= Integer.parseInt(ids[0]);
        int id1=Integer.parseInt(ids[1]);
        if (hostHolder.getUser().getId()==id0) {
            return userService.findUserById(id1);
        }else {
            return userService.findUserById(id0);
        }
    }

    @RequestMapping(path="/letter/list",method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user=hostHolder.getUser();
        if (user==null)
            return "redirect:/login";

        //设置分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        //会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> conversations=new ArrayList<>();
        for (Message message: conversationList) {
            Map<String,Object> map=new HashMap<>();
            map.put("conversation",message);
            int targetId=user.getId()==message.getFromId()?message.getToId():message.getFromId();
            map.put("target",userService.findUserById(targetId));
            map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
            map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(),message.getConversationId()));
            conversations.add(map);
        }
        model.addAttribute("conversations",conversations);
        model.addAttribute("letterUnreadCount",messageService.findLetterUnreadCount(user.getId(),null));
        return "/site/letter";
    }
}
