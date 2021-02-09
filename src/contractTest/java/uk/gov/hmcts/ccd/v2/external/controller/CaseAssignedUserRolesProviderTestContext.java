package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;
import uk.gov.hmcts.ccd.domain.service.cauroles.DefaultCaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

@Configuration
public class CaseAssignedUserRolesProviderTestContext {

    @MockBean
    ApplicationParams applicationParams;

    @MockBean
    SecurityUtils securityUtils;

    @MockBean
    CaseAccessOperation caseAccessOperation;

    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Primary
    @Bean
    protected DefaultCaseAssignedUserRolesOperation caseAssignedUserRolesOperation() {
        return new DefaultCaseAssignedUserRolesOperation(caseAccessOperation);
    }

    @Primary
    @Bean
    protected CaseAssignedUserRolesController caseAssignedUserRolesController() {
        return new CaseAssignedUserRolesController(applicationParams, new UIDService(), caseAssignedUserRolesOperation(), securityUtils);
    }

}
