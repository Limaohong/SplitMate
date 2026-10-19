package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.SettlementResponse;
import com.splitmate.service.SettlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public ApiResponse<SettlementResponse> getSettlement(@PathVariable Long groupId,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(settlementService.getSettlement(groupId, currentUserId));
    }
}
