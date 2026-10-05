package com.splitmate.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class GroupControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateGroupWithCreatorAsFirstMember() throws Exception {
        String aliceAuthorization = registerUserAndGetAuthorization("Alice");

        performCreateGroup(aliceAuthorization, "{\"name\":\"  東京旅行  \",\"currencyCode\":\"JPY\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("東京旅行"))
                .andExpect(jsonPath("$.data.currencyCode").value("JPY"))
                .andExpect(jsonPath("$.data.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.data.members", hasSize(1)))
                .andExpect(jsonPath("$.data.members[0].displayName").value("Alice"));

        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, aliceAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].memberCount").value(1));
    }

    @Test
    void shouldRejectUnsupportedCurrencyAndBlankName() throws Exception {
        String aliceAuthorization = registerUserAndGetAuthorization("Alice");

        performCreateGroup(aliceAuthorization, "{\"name\":\"旅行\",\"currencyCode\":\"ABC\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        performCreateGroup(aliceAuthorization, "{\"name\":\" \",\"currencyCode\":\"TWD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldJoinGroupByInviteCodeIdempotently() throws Exception {
        String aliceAuthorization = registerUserAndGetAuthorization("Alice");
        String bobAuthorization = registerUserAndGetAuthorization("Bob");
        JsonNode createdGroup = createGroup(aliceAuthorization);
        long groupId = createdGroup.path("id").asLong();
        String inviteCode = createdGroup.path("inviteCode").asText();

        // 非成員看不到群組詳細資料
        mockMvc.perform(get("/api/groups/{groupId}", groupId).header(HttpHeaders.AUTHORIZATION, bobAuthorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));

        mockMvc.perform(get("/api/invitations/{inviteCode}", inviteCode)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupName").value("室友合租"))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.isAlreadyMember").value(false));

        // 加入兩次結果相同，不會重複新增成員
        performJoin(bobAuthorization, inviteCode).andExpect(status().isOk());
        performJoin(bobAuthorization, inviteCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members", hasSize(2)))
                .andExpect(jsonPath("$.data.members[1].displayName").value("Bob"));

        mockMvc.perform(get("/api/invitations/{inviteCode}", inviteCode)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthorization))
                .andExpect(jsonPath("$.data.isAlreadyMember").value(true));
        mockMvc.perform(get("/api/groups/{groupId}", groupId).header(HttpHeaders.AUTHORIZATION, bobAuthorization))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidInviteCode() throws Exception {
        String bobAuthorization = registerUserAndGetAuthorization("Bob");

        performJoin(bobAuthorization, "not-exist-code")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    void shouldRequireLoginForGroupApis() throws Exception {
        mockMvc.perform(get("/api/groups")).andExpect(status().isUnauthorized());
    }

    private ResultActions performCreateGroup(String authorization, String requestBody) throws Exception {
        return mockMvc.perform(post("/api/groups")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private JsonNode createGroup(String authorization) throws Exception {
        String responseBody = performCreateGroup(authorization, "{\"name\":\"室友合租\",\"currencyCode\":\"TWD\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("data");
    }

    private ResultActions performJoin(String authorization, String inviteCode) throws Exception {
        return mockMvc.perform(post("/api/invitations/{inviteCode}/join", inviteCode)
                .header(HttpHeaders.AUTHORIZATION, authorization));
    }
}
