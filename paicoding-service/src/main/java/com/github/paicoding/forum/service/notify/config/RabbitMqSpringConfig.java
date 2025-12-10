package com.github.paicoding.forum.service.notify.config;

import com.github.paicoding.forum.core.common.CommonConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqSpringConfig {

    /** 创建统一的 DirectExchange */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(CommonConstants.DIRECT_EXCHANGE, true, false);
    }

    /** 创建队列 */
    @Bean
    public Queue praiseQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_PRAISE, true);
    }

    @Bean
    public Queue collectQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_COLLECT, true);
    }

    @Bean
    public Queue commentQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_COMMENT, true);
    }

    @Bean
    public Queue replyQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_REPLY, true);
    }

    @Bean
    public Queue followQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_FOLLOW, true);
    }

    @Bean
    public Queue payQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_PAY, true);
    }

    @Bean
    public Queue registerQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_REGISTER, true);
    }

    @Bean
    public Queue cancelPraiseQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_CANCEL_PRAISE, true);
    }

    @Bean
    public Queue cancelCollectQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_CANCEL_COLLECT, true);
    }

    @Bean
    public Queue cancelFollowQueue() {
        return new Queue(CommonConstants.QUEUE_NAME_CANCEL_FOLLOW, true);
    }



    /** 创建绑定 */
    @Bean
    public Binding praiseBinding(DirectExchange directExchange, Queue praiseQueue) {
        return BindingBuilder.bind(praiseQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_PRAISE);
    }

    @Bean
    public Binding collectBinding(DirectExchange directExchange, Queue collectQueue) {
        return BindingBuilder.bind(collectQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_COLLECT);
    }

    @Bean
    public Binding commentBinding(DirectExchange directExchange, Queue commentQueue) {
        return BindingBuilder.bind(commentQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_COMMENT);
    }

    @Bean
    public Binding replyBinding(DirectExchange directExchange, Queue replyQueue) {
        return BindingBuilder.bind(replyQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_REPLY);
    }

    @Bean
    public Binding followBinding(DirectExchange directExchange, Queue followQueue) {
        return BindingBuilder.bind(followQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_FOLLOW);
    }

    @Bean
    public Binding payBinding(DirectExchange directExchange, Queue payQueue) {
        return BindingBuilder.bind(payQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_PAY);
    }

    @Bean
    public Binding registerBinding(DirectExchange directExchange, Queue registerQueue) {
        return BindingBuilder.bind(registerQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_REGISTER);
    }
    @Bean
    public Binding cancelPraiseBinding(DirectExchange directExchange, Queue cancelPraiseQueue) {
        return BindingBuilder.bind(cancelPraiseQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_CANCEL_PRAISE);
    }

    @Bean
    public Binding cancelCollectBinding(DirectExchange directExchange, Queue cancelCollectQueue) {
        return BindingBuilder.bind(cancelCollectQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_CANCEL_COLLECT);
    }
    @Bean
    public Binding cancelFollowBinding(DirectExchange directExchange, Queue cancelFollowQueue) {
        return BindingBuilder.bind(cancelFollowQueue)
                .to(directExchange)
                .with(CommonConstants.QUEUE_KEY_CANCEL_FOLLOW);
    }
}
