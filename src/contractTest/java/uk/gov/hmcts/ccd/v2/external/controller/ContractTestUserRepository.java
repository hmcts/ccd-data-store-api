package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.AuthCheckerConfiguration;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;

@Repository
@Qualifier("contractTest")
public class ContractTestUserRepository extends DefaultUserRepository {
    public ContractTestUserRepository(ApplicationParams applicationParams,
                                      @Qualifier("contractTest") ContractTestCaseDefinitionRepository caseDefinitionRepository,
                                      ContractTestSecurityUtils securityUtils, RestTemplate restTemplate,
                                      AuthCheckerConfiguration authCheckerConfiguration) {
        super(applicationParams, caseDefinitionRepository, securityUtils, restTemplate, authCheckerConfiguration);
    }
}
