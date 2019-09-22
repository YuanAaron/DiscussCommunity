package com.oshacker.discusscommunity.event;

import com.alibaba.fastjson.JSONObject;
import com.oshacker.discusscommunity.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    //开发事件的生产者
    public void fireEvent(Event event) {
        //将事件发布到指定的Topic
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
