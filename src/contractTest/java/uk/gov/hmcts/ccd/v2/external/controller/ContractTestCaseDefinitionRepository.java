package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseTypeDefinitionVersion;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefinitionStoreClient;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackUrlValidator;

@Service
@Primary
@Qualifier("cached")
@Profile("SECURITY_MOCK")
public class ContractTestCaseDefinitionRepository extends DefaultCaseDefinitionRepository {
    public ContractTestCaseDefinitionRepository(ApplicationParams applicationParams,
                                                DefinitionStoreClient definitionStoreClient,
                                                CallbackUrlValidator callbackUrlValidator) {
        super(applicationParams, definitionStoreClient, callbackUrlValidator);
    }

    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        CaseTypeDefinitionVersion definitionVersion = new CaseTypeDefinitionVersion();
        definitionVersion.setVersion(0);
        return definitionVersion;
    }

}
