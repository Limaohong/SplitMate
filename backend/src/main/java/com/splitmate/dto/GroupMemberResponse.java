package com.splitmate.dto;

import com.splitmate.entity.GroupMember;

public record GroupMemberResponse(Long userId, String displayName) {

    public static GroupMemberResponse from(GroupMember groupMember) {
        return new GroupMemberResponse(groupMember.getUser().getId(), groupMember.getUser().getDisplayName());
    }
}
