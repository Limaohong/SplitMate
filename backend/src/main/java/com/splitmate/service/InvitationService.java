package com.splitmate.service;

import com.splitmate.dto.GroupDetailResponse;
import com.splitmate.dto.InvitationResponse;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import com.splitmate.repository.ExpenseGroupRepository;
import com.splitmate.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 透過邀請碼預覽與加入群組。
 */
@Service
public class InvitationService {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;

    public InvitationService(ExpenseGroupRepository expenseGroupRepository, GroupMemberRepository groupMemberRepository,
            GroupService groupService) {
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public InvitationResponse getInvitation(String inviteCode, Long currentUserId) {
        ExpenseGroup group = getGroupByInviteCode(inviteCode);
        return new InvitationResponse(
                group.getId(),
                group.getName(),
                group.getCurrencyCode(),
                groupMemberRepository.countByGroupId(group.getId()),
                groupMemberRepository.existsByGroupIdAndUserId(group.getId(), currentUserId));
    }

    /** 加入群組。已是成員時直接回傳群組資料（冪等），使用者重複點邀請連結不會出錯 */
    @Transactional
    public GroupDetailResponse joinGroup(String inviteCode, Long currentUserId) {
        ExpenseGroup group = getGroupByInviteCode(inviteCode);
        groupMemberRepository.insertMemberIfAbsent(group.getId(), currentUserId);
        return groupService.buildGroupDetailResponse(group);
    }

    private ExpenseGroup getGroupByInviteCode(String inviteCode) {
        return expenseGroupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
    }
}
