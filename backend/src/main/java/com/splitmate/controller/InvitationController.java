package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.GroupDetailResponse;
import com.splitmate.dto.InvitationResponse;
import com.splitmate.service.InvitationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations/{inviteCode}")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    public ApiResponse<InvitationResponse> getInvitation(@PathVariable String inviteCode,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(invitationService.getInvitation(inviteCode, currentUserId));
    }

    @PostMapping("/join")
    public ApiResponse<GroupDetailResponse> joinGroup(@PathVariable String inviteCode,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(invitationService.joinGroup(inviteCode, currentUserId));
    }
}
