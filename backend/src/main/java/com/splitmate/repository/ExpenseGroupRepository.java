package com.splitmate.repository;

import com.splitmate.entity.ExpenseGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    Optional<ExpenseGroup> findByInviteCode(String inviteCode);
}
