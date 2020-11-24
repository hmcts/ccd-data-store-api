package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Qualifier("contractTest")
@Singleton
public class ContractTestHttpUIDefinitionGateway extends HttpUIDefinitionGateway {

    @Inject
    public ContractTestHttpUIDefinitionGateway(ApplicationParams applicationParams,
                                               ContractTestSecurityUtils securityUtils,
                                               RestTemplate restTemplate) {
        super(applicationParams, securityUtils, restTemplate);
    }
}
