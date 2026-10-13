package uk.gov.hmcts.ccd.v2.external.controller;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaseAssignedUserRolesControllerContentNegotiationTest {

    private static final String CASE_ID_GOOD = "4444333322221111";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private SecurityUtils securityUtils;

    private MockMvc mockMvc;

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
    }

    @Nested
    @DisplayName("GET /case-users content negotiation")
    class GetCaseUserRolesContentNegotiation {

        @Test
        @DisplayName("should return 200 when Accept header is application/json")
        void getCaseUserRoles_shouldReturn200_whenAcceptHeaderIsApplicationJson() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 when Accept header is application/hal+json")
        void getCaseUserRoles_shouldReturn200_whenAcceptHeaderIsHalJson() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 when Accept header is wildcard")
        void getCaseUserRoles_shouldReturn200_whenAcceptHeaderIsWildcard() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaType.ALL))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 406 when Accept header is unsupported media type")
        void getCaseUserRoles_shouldReturn406_whenAcceptHeaderIsUnsupported() throws Exception {
            mockMvc.perform(get("/case-users")
                    .param("case_ids", CASE_ID_GOOD)
                    .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        }
    }
}
