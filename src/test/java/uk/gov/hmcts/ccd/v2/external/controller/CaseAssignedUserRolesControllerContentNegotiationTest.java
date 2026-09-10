package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies content negotiation across all endpoints in {@link CaseAssignedUserRolesController}.
 *
 * <p>Each endpoint declares explicit {@code produces} media types (application/hal+json and
 * application/json) to work around the Spring Framework concurrency issue #36090. These tests
 * confirm that every endpoint correctly accepts both declared types, honours wildcard requests,
 * and rejects unsupported media types with 406 Not Acceptable.</p>
 */
class CaseAssignedUserRolesControllerContentNegotiationTest {

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String SERVICE_GOOD = "SERVICE_GOOD";
    private static final String S2S_TOKEN_GOOD = "good_s2s_token";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private SecurityUtils securityUtils;

    private MockMvc mockMvc;

    /** Valid JSON payload for POST /case-users and DELETE /case-users. */
    private static final String CASE_USER_ROLES_BODY = """
        {
            "case_users": [
                {
                    "case_id": "4444333322221111",
                    "user_id": "123",
                    "case_role": "[ROLE]"
                }
            ]
        }
        """;

    /** Valid JSON payload for POST /case-users/search. */
    private static final String SEARCH_BODY = """
        {
            "case_ids": ["4444333322221111"],
            "user_ids": ["123"]
        }
        """;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        CaseAssignedUserRolesController controller = new CaseAssignedUserRolesController(
            applicationParams,
            caseReferenceService,
            caseAssignedUserRolesOperation,
            securityUtils
        );

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();

        when(caseReferenceService.validateUID(anyString())).thenReturn(true);
        when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList()))
            .thenReturn(List.of(new CaseAssignedUserRole()));

        // authorise the S2S token for POST/DELETE endpoints
        when(applicationParams.getAuthorisedServicesForCaseUserRoles())
            .thenReturn(Lists.newArrayList(SERVICE_GOOD));
        doReturn(SERVICE_GOOD).when(securityUtils).getServiceNameFromS2SToken(S2S_TOKEN_GOOD);
    }

    // ── GET /case-users ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /case-users content negotiation")
    class GetCaseUserRolesContentNegotiation {

        @Test
        @DisplayName("should return 200 when Accept header is application/json")
        void shouldReturn200_whenAcceptIsApplicationJson() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("should return 200 when Accept header is application/hal+json")
        void shouldReturn200_whenAcceptIsHalJson() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
        }

        @Test
        @DisplayName("should return 200 when Accept header is wildcard")
        void shouldReturn200_whenAcceptIsWildcard() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaType.ALL))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 406 when Accept header is unsupported media type")
        void shouldReturn406_whenAcceptIsUnsupported() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        }
    }

    // ── POST /case-users ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /case-users content negotiation")
    class AddCaseUserRolesContentNegotiation {

        @Test
        @DisplayName("should return 201 when Accept header is application/json")
        void shouldReturn201_whenAcceptIsApplicationJson() throws Exception {
            mockMvc.perform(post("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("should return 201 when Accept header is application/hal+json")
        void shouldReturn201_whenAcceptIsHalJson() throws Exception {
            mockMvc.perform(post("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
        }

        @Test
        @DisplayName("should return 201 when Accept header is wildcard")
        void shouldReturn201_whenAcceptIsWildcard() throws Exception {
            mockMvc.perform(post("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaType.ALL))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 406 when Accept header is unsupported media type")
        void shouldReturn406_whenAcceptIsUnsupported() throws Exception {
            mockMvc.perform(post("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        }
    }

    // ── DELETE /case-users ───────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /case-users content negotiation")
    class RemoveCaseUserRolesContentNegotiation {

        @Test
        @DisplayName("should return 200 when Accept header is application/json")
        void shouldReturn200_whenAcceptIsApplicationJson() throws Exception {
            mockMvc.perform(delete("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("should return 200 when Accept header is application/hal+json")
        void shouldReturn200_whenAcceptIsHalJson() throws Exception {
            mockMvc.perform(delete("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
        }

        @Test
        @DisplayName("should return 200 when Accept header is wildcard")
        void shouldReturn200_whenAcceptIsWildcard() throws Exception {
            mockMvc.perform(delete("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaType.ALL))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 406 when Accept header is unsupported media type")
        void shouldReturn406_whenAcceptIsUnsupported() throws Exception {
            mockMvc.perform(delete("/case-users")
                    .header("ServiceAuthorization", S2S_TOKEN_GOOD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CASE_USER_ROLES_BODY)
                    .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        }
    }

    // ── POST /case-users/search ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /case-users/search content negotiation")
    class SearchCaseUserRolesContentNegotiation {

        @Test
        @DisplayName("should return 200 when Accept header is application/json")
        void shouldReturn200_whenAcceptIsApplicationJson() throws Exception {
            mockMvc.perform(post("/case-users/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SEARCH_BODY)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("should return 200 when Accept header is application/hal+json")
        void shouldReturn200_whenAcceptIsHalJson() throws Exception {
            mockMvc.perform(post("/case-users/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SEARCH_BODY)
                    .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
        }

        @Test
        @DisplayName("should return 200 when Accept header is wildcard")
        void shouldReturn200_whenAcceptIsWildcard() throws Exception {
            mockMvc.perform(post("/case-users/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SEARCH_BODY)
                    .accept(MediaType.ALL))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 406 when Accept header is unsupported media type")
        void shouldReturn406_whenAcceptIsUnsupported() throws Exception {
            mockMvc.perform(post("/case-users/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SEARCH_BODY)
                    .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        }
    }
}
