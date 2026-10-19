package com.splitmate.service;

import com.splitmate.dto.ExpenseResponse;
import com.splitmate.repository.ExpenseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId, Long currentUserId) {
        groupService.getGroupForMember(groupId, currentUserId);
        return expenseRepository.findAllWithSharesByGroupId(groupId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }
}
