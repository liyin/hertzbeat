package com.usthe.manager.service;

import com.usthe.common.entity.alerter.Alert;
import com.usthe.common.entity.manager.NoticeReceiver;
import com.usthe.common.entity.manager.NoticeRule;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Message notification configuration interface
 * 消息通知配置接口
 *
 *
 *
 */
public interface NoticeConfigService {

    /**
     * Dynamic conditional query
     * 动态条件查询
     *
     * @param specification Query conditions    查询条件
     * @return Search result    查询结果
     */
    List<NoticeReceiver> getNoticeReceivers(Specification<NoticeReceiver> specification);

    /**
     * Dynamic conditional query
     * 动态条件查询
     *
     * @param specification Query conditions    查询条件
     * @return Search result    查询结果
     */
    List<NoticeRule> getNoticeRules(Specification<NoticeRule> specification);

    /**
     * Add a notification recipient
     * 新增一个通知接收人
     *
     * @param noticeReceiver recipient information  接收人信息
     */
    void addReceiver(NoticeReceiver noticeReceiver);

    /**
     * Modify notification recipients
     * 修改通知接收人
     *
     * @param noticeReceiver recipient information  接收人信息
     */
    void editReceiver(NoticeReceiver noticeReceiver);

    /**
     * Delete recipient information based on recipient ID
     * 根据接收人ID删除接收人信息
     *
     * @param receiverId Recipient ID   接收人ID
     */
    void deleteReceiver(Long receiverId);

    /**
     * Added notification policy
     * 新增通知策略
     *
     * @param noticeRule Notification strategy  通知策略
     */
    void addNoticeRule(NoticeRule noticeRule);

    /**
     * Modify Notification Policy
     * 修改通知策略
     *
     * @param noticeRule Notification strategy  通知策略
     */
    void editNoticeRule(NoticeRule noticeRule);

    /**
     * Delete the specified notification policy
     * 删除指定的通知策略
     *
     * @param ruleId Notification Policy ID     通知策略ID
     */
    void deleteNoticeRule(Long ruleId);

    /**
     * According to the alarm information matching all notification policies, filter out the recipients who need to be notified
     * 根据告警信息与所有通知策略匹配，过滤出需要通知的接收人
     *
     * @param alert Alarm information       告警信息
     * @return Receiver     接收人
     */
    List<NoticeReceiver> getReceiverFilterRule(Alert alert);
}
