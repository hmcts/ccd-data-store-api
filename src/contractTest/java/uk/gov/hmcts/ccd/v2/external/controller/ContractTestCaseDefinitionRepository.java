package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefinitionStoreClient;

@Service
@Primary
@Qualifier("cached")
@Profile("SECURITY_MOCK")
public class ContractTestCaseDefinitionRepository extends DefaultCaseDefinitionRepository {
    public ContractTestCaseDefinitionRepository(ApplicationParams applicationParams,
                                                DefinitionStoreClient definitionStoreClient) {
        super(applicationParams, definitionStoreClient);
    }
}
