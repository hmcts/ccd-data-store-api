package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.ccd.data.definition.CachedUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionGateway;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
@Primary
public class ContractTestCachedUIDefinitionGateway extends CachedUIDefinitionGateway {

    public ContractTestCachedUIDefinitionGateway(HttpUIDefinitionGateway httpUiDefinitionGateway) {
        super(httpUiDefinitionGateway);
    }
}
