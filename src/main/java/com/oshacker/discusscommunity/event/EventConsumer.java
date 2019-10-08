package com.oshacker.discusscommunity.event;

import com.alibaba.fastjson.JSONObject;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Event;
import com.oshacker.discusscommunity.entity.Message;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.service.ElasticsearchService;
import com.oshacker.discusscommunity.service.MessageService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sun.misc.resources.Messages;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements DiscussCommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleMessage(ConsumerRecord record) {
        if (record==null || record.value()==null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息的格式错误!");
            return;
        }

        //发送站内通知
        Message message=new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String,Object> content=new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String,Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record==null || record.value()==null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息的格式错误!");
            return;
        }

        DiscussPost discussPost=discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record==null || record.value()==null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息的格式错误!");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record) {
        if (record==null || record.value()==null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息的格式错误!");
            return;
        }

        String htmlUrl= (String) event.getData().get("htmlUrl");
        String fileName= (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd=wkImageCommand+" --quality 75 "+htmlUrl+" "+wkImageStorage+"/"+fileName+suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功："+cmd);
        } catch (IOException e) {
            logger.error("生成长图失败："+e.getMessage());
        }
    }
}
