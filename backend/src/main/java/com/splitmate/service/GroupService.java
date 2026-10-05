package com.splitmate.service;

import com.splitmate.dto.CreateGroupRequest;
import com.splitmate.dto.GroupDetailResponse;
import com.splitmate.dto.GroupMemberResponse;
import com.splitmate.dto.GroupSummaryResponse;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.GroupMember;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import com.splitmate.repository.ExpenseGroupRepository;
import com.splitmate.repository.GroupMemberRepository;
import com.splitmate.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {

    /** 16 bytes = 128 bits 隨機數，Base64URL 編碼後 22 字元，可安全放在網址中且無法被猜測 */
    private static final int INVITE_CODE_RANDOM_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder INVITE_CODE_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(ExpenseGroupRepository expenseGroupRepository, GroupMemberRepository groupMemberRepository,
            UserRepository userRepository) {
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    /** 建立群組，建立者自動成為第一位成員 */
    @Transactional
    public GroupDetailResponse createGroup(CreateGroupRequest createGroupRequest, Long currentUserId) {
        // getReferenceById 只建立代理物件、不發 SELECT；userId 來自已驗證的 JWT，可信任其存在
        User currentUser = userRepository.getReferenceById(currentUserId);
        ExpenseGroup newGroup = expenseGroupRepository.save(new ExpenseGroup(
                createGroupRequest.name().trim(), createGroupRequest.currencyCode(), generateInviteCode(), currentUser));
        groupMemberRepository.save(new GroupMember(newGroup, currentUser));
        return buildGroupDetailResponse(newGroup);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> getMyGroups(Long currentUserId) {
        return groupMemberRepository.findGroupSummariesByUserId(currentUserId);
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(Long groupId, Long currentUserId) {
        return buildGroupDetailResponse(getGroupForMember(groupId, currentUserId));
    }

    /**
     * 取得群組並確認目前使用者是成員；群組相關功能（支出、結算）都應先經過這個檢查。
     * 群組不存在與「不是成員」回傳相同錯誤，避免外人藉由 ID 探測有哪些群組。
     */
    @Transactional(readOnly = true)
    public ExpenseGroup getGroupForMember(Long groupId, Long currentUserId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        return expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
    }

    /** 須在交易內呼叫（呼叫端皆已標註 @Transactional），才能讀取 LAZY 關聯 */
    public GroupDetailResponse buildGroupDetailResponse(ExpenseGroup group) {
        List<GroupMemberResponse> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId())
                .stream()
                .map(GroupMemberResponse::from)
                .toList();
        return GroupDetailResponse.of(group, members);
    }

    private String generateInviteCode() {
        byte[] randomBytes = new byte[INVITE_CODE_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return INVITE_CODE_ENCODER.encodeToString(randomBytes);
    }
}
