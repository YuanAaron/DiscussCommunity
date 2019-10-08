package com.oshacker.discusscommunity.event;

import com.alibaba.fastjson.JSONObject;
import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.entity.Event;
import com.oshacker.discusscommunity.entity.Message;
import com.oshacker.discusscommunity.service.DiscussPostService;
import com.oshacker.discusscommunity.service.ElasticsearchService;
import com.oshacker.discusscommunity.service.MessageService;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import sun.misc.resources.Messages;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

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

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

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

        //分布式环境下，不能使用ThreadPoolTaskScheduler，那么为什么这里可以用ThreadPoolTaskScheduler？
        //假设分布式部署在5台服务器，且每台服务器上都部署了同样的Consumer，但是消费者存在抢占的机制，
        //因此事件只可能被一个Consumer消费，别的Consumer不会处理，即Consumer之间互相排斥，这是消息队列都有的机制。
        //所以，即使在分布式环境下，使用ThreadPoolTaskScheduler启动定时任务也没有问题。

        //启动定时器，监视该图片，一旦图片生成了就上传至七牛云
        UploadTask task=new UploadTask(fileName,suffix);
        //future封装了任务的状态，也可以用来停止定时器
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);//500ms
        task.setFuture(future); //启动定时器500ms后，run才开始执行，因此先执行setFuture.
    }

    class UploadTask implements Runnable {

        //上传的文件名
        private String fileName;
        //上传的文件后缀
        private String suffix;
        //启动定时任务的返回值：用于完成任务后关闭定时器
        private Future future;
        //上传的时候一定要考虑可用性的问题,对下面两种情况要有兜底的方案：
        //1、图片一直没有生成（比如Runtime.getRuntime().exec(cmd)执行失败），一直没有上传成功，也就无法停止定时器；
        //2、图片生成了，但是上传到七牛云失败了，可能网络有问题或者七牛云的服务器挂了等，一直没有上传成功，总不能一直传吧
        //开始时间
        private long startTime;
        //上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime=System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            //生成图片失败
            if (System.currentTimeMillis()-startTime>30000) {
                logger.error("执行时间过长，终止任务："+fileName);
                future.cancel(true); //停止定时器
                return;
            }

            //生成图片成功，上传失败
            if (uploadTimes>=3) {
                logger.error("上传次数过多，终止任务："+fileName);
                future.cancel(true); //停止定时器
                return;
            }

            String localPath=wkImageStorage+"/"+fileName+suffix;
            File file=new File(localPath);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].",++uploadTimes,fileName));
                //生成上传凭证
                //1、设置响应信息
                StringMap policy=new StringMap();
                policy.put("returnBody",DiscussCommunityUtil.getJSONString(0));
                //2、生成上传凭证
                Auth auth=Auth.create(accessKey,secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);//1h内有效
                //指定上传的机房
                UploadManager manager=new UploadManager(new Configuration(Zone.zone1()));
                try {
                    //开始上传图片
                    Response response = manager.put(
                            localPath, fileName, uploadToken,null,
                            "image/" + suffix.substring(suffix.lastIndexOf(".")+1), false);
                    //处理响应结果
                    JSONObject json=JSONObject.parseObject(response.bodyString());
                    if (json==null || json.get("code")==null || !json.get("code").toString().equals("0")) {
                        logger.error(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s]",uploadTimes,fileName));
                        future.cancel(true); //停止定时器
                    }
                } catch (QiniuException e) {
                    logger.error(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                }

            } else {
                logger.info("等待图片生成["+fileName+"].");
            }

        }
    }
}
