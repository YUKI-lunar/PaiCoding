package com.github.paicoding.forum.test.rabbitmq;

import com.github.paicoding.forum.core.common.CommonConstants;
import com.github.paicoding.forum.service.notify.help.RabbitNotifyHelper;
import com.github.paicoding.forum.test.BasicTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitmqTest extends BasicTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Test
    public void testProductRabbitmq() {
            RabbitNotifyHelper.publishEvent(CommonConstants.DIRECT_EXCHANGE,
                    CommonConstants.QUEUE_KEY_PRAISE,
                    "lvmenglou test msg");
    }

    @Test
    @RabbitListener(queues = "praiseQueue")
    public void testConsumerRabbitmq() {
        System.out.println("可以");;
    }
}
