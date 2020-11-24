package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionGateway;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Qualifier("contractTest")
@Singleton
public class ContractTestCachedUIDefinitionGateway extends CachedUIDefinitionGateway {

    public ContractTestCachedUIDefinitionGateway(@Qualifier("contractTest") HttpUIDefinitionGateway httpUiDefinitionGateway) {
        super(httpUiDefinitionGateway);
    }
}
