package uk.gov.hmcts.ccd.v2.external.controller.caseassigned;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;
import uk.gov.hmcts.ccd.domain.service.cauroles.DefaultCaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController;

@Configuration
@Profile("CASE_ASSIGNED")
public class CaseAssignedUserRolesProviderTestContext {

    @MockBean
    ApplicationParams applicationParams;

    @MockBean
    SecurityUtils securityUtils;

    @MockBean
    CaseAccessOperation caseAccessOperation;

    @Primary
    @Bean
    protected DefaultCaseAssignedUserRolesOperation caseAssignedUserRolesOperation() {
        return new DefaultCaseAssignedUserRolesOperation(caseAccessOperation);
    }

    @Primary
    @Bean
    protected CaseAssignedUserRolesController caseAssignedUserRolesController() {
        return new CaseAssignedUserRolesController(applicationParams,
            new UIDService(), caseAssignedUserRolesOperation(), securityUtils);
    }

}
