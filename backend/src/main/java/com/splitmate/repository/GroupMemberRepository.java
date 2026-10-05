package com.splitmate.repository;

import com.splitmate.dto.GroupSummaryResponse;
import com.splitmate.entity.GroupMember;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    /** 一併載入 user，避免顯示成員名稱時每位成員各發一次查詢（N+1） */
    @EntityGraph(attributePaths = "user")
    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(Long groupId);

    /**
     * 已是成員就不做事，回傳實際新增的筆數（0 或 1）。
     * 用 PostgreSQL 的 ON CONFLICT 在單一 SQL 內完成「檢查 + 新增」，同一人同時送出兩次加入請求也不會出錯。
     * 不能改用「save 後 catch 唯一鍵例外」：PostgreSQL 交易內一旦發生錯誤，後續所有查詢都會被拒絕直到 rollback。
     */
    @Modifying
    @Query(value = """
            INSERT INTO group_members (group_id, user_id)
            VALUES (:groupId, :userId)
            ON CONFLICT (group_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertMemberIfAbsent(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("""
            SELECT new com.splitmate.dto.GroupSummaryResponse(
                g.id, g.name, g.currencyCode,
                (SELECT COUNT(otherMember) FROM GroupMember otherMember WHERE otherMember.group = g))
            FROM GroupMember member
            JOIN member.group g
            WHERE member.user.id = :userId
            ORDER BY g.createdAt DESC
            """)
    List<GroupSummaryResponse> findGroupSummariesByUserId(@Param("userId") Long userId);
}
