package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;

@Service
@Primary
public class ContractTestCaseDefinitionRepository extends DefaultCaseDefinitionRepository {
    public ContractTestCaseDefinitionRepository(ApplicationParams applicationParams,
                                                SecurityUtils securityUtils,
                                                RestTemplate restTemplate) {
        super(applicationParams, securityUtils, restTemplate);
    }
}
