package com.github.paicoding.forum.service.notify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.github.paicoding.forum.api.model.enums.NotifyTypeEnum;
import com.github.paicoding.forum.api.model.enums.pay.PayStatusEnum;
import com.github.paicoding.forum.api.model.enums.pay.ThirdPayWayEnum;
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.core.common.CommonConstants;
import com.github.paicoding.forum.api.model.enums.DocumentTypeEnum;
import com.github.paicoding.forum.api.model.enums.NotifyStatEnum;
import com.github.paicoding.forum.core.util.SpringUtil;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.article.repository.entity.ArticlePayRecordDO;
import com.github.paicoding.forum.service.article.service.ArticleReadService;
import com.github.paicoding.forum.service.comment.repository.entity.CommentDO;
import com.github.paicoding.forum.service.comment.service.CommentReadService;
import com.github.paicoding.forum.service.notify.repository.dao.NotifyMsgDao;
import com.github.paicoding.forum.service.notify.repository.entity.NotifyMsgDO;
import com.github.paicoding.forum.service.notify.service.NotifyService;
import com.github.paicoding.forum.service.user.repository.entity.UserFootDO;
import com.github.paicoding.forum.service.user.repository.entity.UserRelationDO;

import com.github.paicoding.forum.service.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Async
@Component
public class RabbitmqListener {

    private static final Long ADMIN_ID = 1L;

    private final ArticleReadService articleReadService;
    private final CommentReadService commentReadService;
    private final NotifyMsgDao notifyMsgDao;
    private final NotifyService notifyService;
    private final UserService userService;

    public RabbitmqListener(ArticleReadService articleReadService,
                            CommentReadService commentReadService,
                            NotifyService notifyService,
                            NotifyMsgDao notifyMsgDao,
                            UserService userService) {
        this.articleReadService = articleReadService;
        this.commentReadService = commentReadService;
        this.notifyService = notifyService;
        this.notifyMsgDao = notifyMsgDao;
        this.userService = userService;
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_COMMENT)
    public void handleComment(CommentDO comment) {
        saveCommentNotify(comment);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_REPLY)
    public void handleReply(CommentDO comment) {
        saveReplyNotify(comment);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_PRAISE)
    public void handlePraise(UserFootDO foot) {
        saveArticleNotify(foot, NotifyTypeEnum.PRAISE);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_COLLECT)
    public void handleCollect(UserFootDO foot) {
        saveArticleNotify(foot, NotifyTypeEnum.COLLECT);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_CANCEL_PRAISE)
    public void handleCancelPraise(UserFootDO foot) {
        removeArticleNotify(foot, NotifyTypeEnum.CANCEL_PRAISE);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_CANCEL_COLLECT)
    public void handleCancelCollect(UserFootDO foot) {
        removeArticleNotify(foot, NotifyTypeEnum.CANCEL_COLLECT);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_FOLLOW)
    public void handleFollow(UserRelationDO relation) {
        saveFollowNotify(relation);
    }

    @RabbitListener(queues = CommonConstants.QUEUE_NAME_CANCEL_FOLLOW)
    public void handleCancelFollow(UserRelationDO relation) {
        removeFollowNotify(relation);
    }



    /**
     * 评论消息
     */
    private void saveCommentNotify(CommentDO comment) {
        NotifyMsgDO msg = new NotifyMsgDO();
        ArticleDO article = articleReadService.queryBasicArticle(comment.getArticleId());
        msg.setNotifyUserId(article.getUserId())
                .setOperateUserId(comment.getUserId())
                .setRelatedId(article.getId())
                .setType(NotifyTypeEnum.COMMENT.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg(comment.getContent());
        // 对于评论而言，支持多次评论
        notifyMsgDao.save(msg);

        // 消息通知
        notifyService.notifyToUser(msg.getNotifyUserId(),
                String.format("您的文章《%s》收到一个新的评论，快去看看吧", article.getTitle()));
    }

    /**
     * 评论回复消息
     */
    private void saveReplyNotify(CommentDO comment) {
        NotifyMsgDO msg = new NotifyMsgDO();
        CommentDO parent = commentReadService.queryComment(comment.getParentCommentId());
        msg.setNotifyUserId(parent.getUserId())
                .setOperateUserId(comment.getUserId())
                .setRelatedId(comment.getArticleId())
                .setType(NotifyTypeEnum.REPLY.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg(comment.getContent());
        // 回复支持多次
        notifyMsgDao.save(msg);

        // 消息通知
        notifyService.notifyToUser(msg.getNotifyUserId(),
                String.format("您的评价《%s》收到一个新的回复，快去看看吧", parent.getContent()));
    }

    /**
     * 点赞 + 收藏
     */
    private void saveArticleNotify(UserFootDO foot, NotifyTypeEnum notifyTypeEnum) {
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(foot.getDocumentId())
                .setNotifyUserId(foot.getDocumentUserId())
                .setOperateUserId(foot.getUserId())
                .setType(notifyTypeEnum.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg("");
        if (Objects.equals(foot.getDocumentType(), DocumentTypeEnum.COMMENT.getCode())) {
            CommentDO comment = commentReadService.queryComment(foot.getDocumentId());
            ArticleDO article = articleReadService.queryBasicArticle(comment.getArticleId());
            msg.setMsg(String.format("赞了您在文章 <a href=\"/article/detail/%d\">%s</a> 下的评论 <span style=\"color:darkslategray;font-style: italic;font-size: 0.9em\">%s</span>",
                    article.getId(), article.getTitle(), comment.getContent()));
        }

        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            notifyMsgDao.save(msg);
            notifyService.notifyToUser(msg.getNotifyUserId(),
                    String.format("太棒了，您的%s %s数+1!!!",
                            Objects.equals(foot.getDocumentType(), DocumentTypeEnum.ARTICLE.getCode()) ? "文章" : "评论",
                            notifyTypeEnum.getMsg()));
        }
    }

    /**
     * 取消点赞、取消收藏
     */
    private void removeArticleNotify(UserFootDO foot, NotifyTypeEnum notifyTypeEnum) {
        NotifyMsgDO msg = new NotifyMsgDO()
                .setRelatedId(foot.getDocumentId())
                .setNotifyUserId(foot.getDocumentUserId())
                .setOperateUserId(foot.getUserId())
                .setType(notifyTypeEnum.getType())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record != null) {
            notifyMsgDao.removeById(record.getId());
        }
    }

    /**
     * 关注
     */
    private void saveFollowNotify(UserRelationDO relation) {
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(0L)
                .setNotifyUserId(relation.getUserId())
                .setOperateUserId(relation.getFollowUserId())
                .setType(NotifyTypeEnum.FOLLOW.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            notifyMsgDao.save(msg);
            notifyService.notifyToUser(msg.getNotifyUserId(), "恭喜您获得一枚新粉丝~");
        }
    }

    /**
     * 取消关注
     */
    private void removeFollowNotify(UserRelationDO relation) {
        NotifyMsgDO msg = new NotifyMsgDO()
                .setRelatedId(0L)
                .setNotifyUserId(relation.getUserId())
                .setOperateUserId(relation.getFollowUserId())
                .setType(NotifyTypeEnum.CANCEL_FOLLOW.getType())
                .setMsg("");
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record != null) {
            notifyMsgDao.removeById(record.getId());
        }
    }

    /**
     * 注册系统通知
     */
    private void saveRegisterSystemNotify(Long userId) {
        NotifyMsgDO msg = new NotifyMsgDO().setRelatedId(0L)
                .setNotifyUserId(userId)
                .setOperateUserId(ADMIN_ID)
                .setType(NotifyTypeEnum.REGISTER.getType())
                .setState(NotifyStatEnum.UNREAD.getStat())
                .setMsg(SpringUtil.getConfig("view.site.welcomeInfo"));
        NotifyMsgDO record = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (record == null) {
            notifyMsgDao.save(msg);
            notifyService.notifyToUser(msg.getNotifyUserId(), "您有一个新的系统通知消息，请注意查收");
        }
    }

    /**
     * 支付通知
     */
    private void savePayNotify(ArticlePayRecordDO record) {
        ArticleDO article = articleReadService.queryBasicArticle(record.getArticleId());

        NotifyMsgDO msg;
        PayStatusEnum payStatus = PayStatusEnum.statusOf(record.getPayStatus());
        if (PayStatusEnum.PAYING == payStatus) {
            BaseUserInfoDTO payUser = userService.queryBasicUserInfo(record.getPayUserId());
            msg = new NotifyMsgDO().setRelatedId(record.getArticleId())
                    .setNotifyUserId(record.getReceiveUserId())
                    .setOperateUserId(record.getPayUserId())
                    .setType(NotifyTypeEnum.PAY.getType())
                    .setState(NotifyStatEnum.UNREAD.getStat())
                    .setMsg(String.format("您的文章 <a href=\"/article/detail/%d\">%s</a> 收到一份来自 <a href=\"/user/home?userId=%d\">%s</a> 的 [%s] 打赏，点击 <a href=\"/article/payConfirm?payId=%d\">去确认~</a>",
                            record.getArticleId(), article.getTitle(),
                            payUser.getUserId(), payUser.getUserName(),
                            StringUtils.isBlank(record.getPayWay()) || Objects.equals(record.getPayWay(), ThirdPayWayEnum.EMAIL.getPay()) ? "个人收款码" : "微信支付",
                            record.getId()));
        } else {
            msg = new NotifyMsgDO().setRelatedId(record.getArticleId())
                    .setNotifyUserId(record.getPayUserId())
                    .setOperateUserId(record.getReceiveUserId())
                    .setType(NotifyTypeEnum.PAY.getType())
                    .setState(NotifyStatEnum.UNREAD.getStat())
                    .setMsg(
                            PayStatusEnum.SUCCEED == payStatus
                                    ? String.format("您对 <a href=\"/article/detail/%d\">%s</a> 的支付已完成~", record.getArticleId(), article.getTitle())
                                    : String.format("您对 <a href=\"/article/detail/%d\">%s</a> 的支付未完成哦~", record.getArticleId(), article.getTitle())
                    );
        }

        NotifyMsgDO dbMsg = notifyMsgDao.getByUserIdRelatedIdAndType(msg);
        if (dbMsg == null || !Objects.equals(dbMsg.getMsg(), msg.getMsg()) || (payStatus == PayStatusEnum.PAYING && Objects.equals(dbMsg.getState(), NotifyStatEnum.UNREAD.getStat()))) {
            notifyMsgDao.save(msg);
        }
        // 发送通知
        if (payStatus == PayStatusEnum.PAYING) {
            notifyService.notifyToUser(msg.getNotifyUserId(), String.format("您的文章《%s》收到一份打赏，请及时确认~", article.getTitle()));
        } else if (payStatus == PayStatusEnum.SUCCEED) {
            notifyService.notifyToUser(msg.getNotifyUserId(), String.format("您对文章《%s》的支付已完成，刷新即可阅读全文哦~", article.getTitle()));
        } else if (payStatus == PayStatusEnum.FAIL) {
            notifyService.notifyToUser(msg.getNotifyUserId(), String.format("您对文章《%s》的支付未成功，请重试一下吧~", article.getTitle()));
        }

    }
}