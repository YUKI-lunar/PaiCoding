package com.github.paicoding.forum.service.notify.help;

import com.github.paicoding.forum.core.util.SpringUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitNotifyHelper {

    private RabbitTemplate rabbitTemplate;
    public RabbitNotifyHelper(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public <T> void publish(String exchange, String routingKey, T content) {
        rabbitTemplate.convertAndSend(exchange,routingKey,content);
        System.out.println("rabbitMq");
    }

    public static <T> void publishEvent(String exchange, String routingKey, T content){
        SpringUtil.getBean(RabbitNotifyHelper.class).publish(exchange,routingKey,content);
    }
}
