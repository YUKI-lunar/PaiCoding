package com.github.paicoding.forum.service.notify.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
@Component
public class KafkaListenerConsumer {

    @KafkaListener(topics = "topic.praise")
    public void handlePraiseTopic(String foot) throws Exception{
        System.out.println("kafka开始消费"+foot);
        //todo 这里没有实现功能,只是预留了一个kafka的消费
    }
}
