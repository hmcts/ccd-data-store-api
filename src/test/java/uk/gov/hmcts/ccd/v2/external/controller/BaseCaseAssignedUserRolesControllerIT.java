package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

class BaseCaseAssignedUserRolesControllerIT extends WireMockBaseTest {

    protected static final Long INVALID_CASE_ID = 222L;

    protected static final String AUTHORISED_ADD_SERVICE_1 = "ADD_SERVICE_1";
    protected static final String AUTHORISED_ADD_SERVICE_2 = "ADD_SERVICE_2";
    protected static final String UNAUTHORISED_ADD_SERVICE = "UNAUTHORISED_ADD_SERVICE";

    protected static final String CASE_ID_1 = "7578590391163133";
    protected static final String CASE_ID_2 = "6375837333991692";
    protected static final String CASE_IDS = CASE_ID_1 + "," + CASE_ID_2;

    protected static final String CASE_ID_EXTRA = "1983927457663329";

    protected static final String CASE_ROLE_1 = "[case-role-1]";
    protected static final String CASE_ROLE_2 = "[case-role-2]";
    protected static final String INVALID_CASE_ROLE = "bad-role";

    protected static final String USER_IDS_1 = "89000";
    protected static final String USER_IDS_2 = "89001";
    protected static final String USER_IDS_3 = "89002";
    protected static final String USER_IDS = USER_IDS_1 + "," + USER_IDS_2;

    protected static final String INVALID_USER_IDS = USER_IDS_1 + ", ," + USER_IDS_2;

    protected static final String ORGANISATION_ID_1 = "OrgA";
    protected static final String ORGANISATION_ID_2 = "OrgB";
    protected static final String INVALID_ORGANISATION_ID = "";

    protected static final String ORGANISATION_ASSIGNED_USER_COUNTER_KEY = "orgs_assigned_users";

    protected final String caseworkerCaa = "caseworker-caa";
    protected final String getCaseAssignedUserRoles = "/case-users";
    protected final String postCaseAssignedUserRoles = "/case-users";

    protected static final String PARAM_CASE_IDS = "case_ids";
    protected static final String PARAM_USER_IDS = "user_ids";

    @Inject
    protected ApplicationParams applicationParams;

    @SpyBean @Inject
    protected AuditRepository auditRepository;

    @Mock
    protected Authentication authentication;

    @Mock
    protected SecurityContext securityContext;

    protected MockMvc mockMvc;

    @Inject @Qualifier(DefaultCaseUserRepository.QUALIFIER)
    protected CaseUserRepository caseUserRepository;

    @Inject @Qualifier("default")
    protected SupplementaryDataRepository supplementaryDataRepository;

    @Inject
    protected WebApplicationContext wac;

    @BeforeEach
    void setUp() throws IOException {
        super.initMock();
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        ReflectionTestUtils.setField(
            applicationParams,
            "authorisedServicesForCaseUserRoles",
            Lists.newArrayList(AUTHORISED_ADD_SERVICE_1, AUTHORISED_ADD_SERVICE_2)
        );
        // disable suppression of audit logs
        ReflectionTestUtils.setField(
            applicationParams,
            "auditLogIgnoreStatuses",
            Lists.newArrayList()
        );

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String userJson = "{\n"
            + "          \"sub\": \"Cloud.Strife@test.com\",\n"
            + "          \"uid\": \"89000\",\n"
            + "          \"roles\": [\n"
            + "            \"caseworker\",\n"
            + "            \"caseworker-probate-public\"\n"
            + "          ],\n"
            + "          \"name\": \"Cloud Strife\"\n"
            + "        }";
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(userJson).withStatus(200)));
    }

    protected HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        String s2SToken = MockUtils.generateDummyS2SToken(AUTHORISED_ADD_SERVICE_1);
        headers.add(SERVICE_AUTHORIZATION, "Bearer " + s2SToken);
        return headers;
    }

    protected void verifyAuditForAddCaseUserRoles(HttpStatus status,
                                                  List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(),
                is(AuditOperationType.ADD_CASE_ASSIGNED_USER_ROLES.getLabel()));
        assertThat(captor.getValue().getHttpStatus(), is(status.value()));
        assertThat(captor.getValue().getPath(), is(postCaseAssignedUserRoles));
        assertThat(captor.getValue().getHttpMethod(), is(HttpMethod.POST.name()));

        if (caseUserRoles != null) {
            String caseIds = caseUserRoles.stream().map(CaseAssignedUserRoleWithOrganisation::getCaseDataId)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));
            String caseRoles = caseUserRoles.stream().map(CaseAssignedUserRoleWithOrganisation::getCaseRole)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));
            String userIds = caseUserRoles.stream().map(CaseAssignedUserRoleWithOrganisation::getUserId)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));

            assertThat(captor.getValue().getCaseId(), is(caseIds));
            assertThat(StringUtils.join(captor.getValue().getTargetCaseRoles(), CASE_ID_SEPARATOR), is(caseRoles));
            assertThat(captor.getValue().getTargetIdamId(), is(userIds));
        }
    }

    protected void verifyAuditForRemoveCaseUserRoles(HttpStatus status,
                                                     List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getValue().getOperationType(),
            is(AuditOperationType.REMOVE_CASE_ASSIGNED_USER_ROLES.getLabel()));
        assertThat(captor.getValue().getHttpStatus(), is(status.value()));
        assertThat(captor.getValue().getPath(), is(postCaseAssignedUserRoles));
        assertThat(captor.getValue().getHttpMethod(), is(HttpMethod.DELETE.name()));

        if (caseUserRoles != null) {
            String caseIds = caseUserRoles.stream().map(CaseAssignedUserRoleWithOrganisation::getCaseDataId)
                    .collect(Collectors.joining(CASE_ID_SEPARATOR));
            String caseRoles = caseUserRoles.stream().map(CaseAssignedUserRoleWithOrganisation::getCaseRole)
                    .collect(Collectors.joining(CASE_ID_SEPARATOR));
            String userIds = caseUserRoles.stream().map(CaseAssignedUserRoleWithOrganisation::getUserId)
                    .collect(Collectors.joining(CASE_ID_SEPARATOR));

            assertThat(captor.getValue().getCaseId(), is(caseIds));
            assertThat(StringUtils.join(captor.getValue().getTargetCaseRoles(), CASE_ID_SEPARATOR), is(caseRoles));
            assertThat(captor.getValue().getTargetIdamId(), is(userIds));
        }
    }

    protected void verifyAuditForGetCaseUserRoles(HttpStatus status, String caseIds, String userIds) {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(),
            is(AuditOperationType.GET_CASE_ASSIGNED_USER_ROLES.getLabel()));
        assertThat(captor.getValue().getHttpStatus(), is(status.value()));
        assertThat(captor.getValue().getPath(), is(getCaseAssignedUserRoles));
        assertThat(captor.getValue().getHttpMethod(), is(HttpMethod.GET.name()));

        if (caseIds != null) {
            assertThat(captor.getValue().getCaseId(), is(trimSpacesFromCsvValues(caseIds)));
        }
        if (userIds != null) {
            assertThat(captor.getValue().getTargetIdamId(), is(trimSpacesFromCsvValues(userIds)));
        }
    }

    protected String trimSpacesFromCsvValues(String csvInput) {
        return Arrays.stream(csvInput.split(CASE_ID_SEPARATOR))
            .map(String::trim)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    protected String getOrgUserCountSupDataKey(String organisationId) {
        return String.format("%s.%s", ORGANISATION_ASSIGNED_USER_COUNTER_KEY,  organisationId);
    }

    protected long getOrgUserCountFromSupData(String caseId, String organisationId) {
        String orgCountSupDataKey = getOrgUserCountSupDataKey(organisationId);

        try {
            return Long.parseLong(
                supplementaryDataRepository.findSupplementaryData(caseId, Collections.singleton(orgCountSupDataKey))
                    .getResponse().getOrDefault(orgCountSupDataKey, 0L).toString()
            );
        } catch (IllegalArgumentException e) {
            return 0L;
        }
    }

}
