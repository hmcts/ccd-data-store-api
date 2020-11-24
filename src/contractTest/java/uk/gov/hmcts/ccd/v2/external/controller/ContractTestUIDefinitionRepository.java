package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
@Qualifier("contractTest")
public class ContractTestUIDefinitionRepository extends UIDefinitionRepository {
    public ContractTestUIDefinitionRepository( @Qualifier("contractTest") CaseDefinitionRepository caseDefinitionRepository,
                                               @Qualifier("contractTest") CachedUIDefinitionGateway cachedUiDefinitionGateway) {
        super(caseDefinitionRepository, cachedUiDefinitionGateway);
    }
}
