package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.CreateGroupRequest;
import com.splitmate.dto.GroupDetailResponse;
import com.splitmate.dto.GroupSummaryResponse;
import com.splitmate.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupDetailResponse> createGroup(@Valid @RequestBody CreateGroupRequest createGroupRequest,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(groupService.createGroup(createGroupRequest, currentUserId));
    }

    @GetMapping
    public ApiResponse<List<GroupSummaryResponse>> getMyGroups(@CurrentUserId Long currentUserId) {
        return ApiResponse.success(groupService.getMyGroups(currentUserId));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> getGroupDetail(@PathVariable Long groupId,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(groupService.getGroupDetail(groupId, currentUserId));
    }
}
