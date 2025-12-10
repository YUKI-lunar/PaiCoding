package com.github.paicoding.forum.api.model.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum NotifyTypeEnum {
    COMMENT(1, "评论", "queue.comment", "comment"),
    REPLY(2, "回复", "queue.reply", "reply"),
    PRAISE(3, "点赞", "queue.praise", "praise"),
    COLLECT(4, "收藏", "queue.collect", "collect"),
    FOLLOW(5, "关注消息", "queue.follow", "follow"),
    SYSTEM(6, "系统消息", "", ""),

    DELETE_COMMENT(1, "删除评论", "", ""),
    DELETE_REPLY(2, "删除回复", "", ""),
    CANCEL_PRAISE(3, "取消点赞", "queue.cancelPraise", "cancelPraise"),
    CANCEL_COLLECT(4, "取消收藏", "queue.cancelCollect", "cancelCollect"),
    CANCEL_FOLLOW(5, "取消关注", "queue.cancelFollow", "cancelFollow"),

    // 注册、登录添加系统相关提示消息
    REGISTER(6, "用户注册", "queue.register", "register"),
    BIND(6, "绑定星球", "", ""),
    LOGIN(6, "用户登录", "", ""),

    PAYING(6, "支付中通知", "queue.pay", "pay"),
    PAY(6, "支付结果通知", "queue.pay", "pay"),
    ;

    private final int type;
    private final String msg;
    private final String queueName; // 队列名
    private final String queueKey;  // 队列 routing key

    private static final Map<Integer, NotifyTypeEnum> mapper = new HashMap<>();

    static {
        for (NotifyTypeEnum type : values()) {
            mapper.put(type.type, type);
        }
    }

    NotifyTypeEnum(int type, String msg, String queueName, String queueKey) {
        this.type = type;
        this.msg = msg;
        this.queueName = queueName;
        this.queueKey = queueKey;
    }

    public static NotifyTypeEnum typeOf(int type) {
        return mapper.get(type);
    }

    public static NotifyTypeEnum typeOf(String type) {
        return valueOf(type.toUpperCase().trim());
    }
}
