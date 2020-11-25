package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.ccd.data.definition.CachedUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
@Primary
public class ContractTestUIDefinitionRepository extends UIDefinitionRepository {
    public ContractTestUIDefinitionRepository( CaseDefinitionRepository caseDefinitionRepository,
                                               CachedUIDefinitionGateway cachedUiDefinitionGateway) {
        super(caseDefinitionRepository, cachedUiDefinitionGateway);
    }
}
