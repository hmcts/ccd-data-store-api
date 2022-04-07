package uk.gov.hmcts.ccd.domain.service.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkService;

import java.util.List;

@Service
public class CaseLinkMigrationService {

    private final CaseLinkService caseLinkService;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public CaseLinkMigrationService(CaseLinkService caseLinkService,
                                    @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
                                    CaseDefinitionRepository caseDefinitionRepository) {
        this.caseLinkService = caseLinkService;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public void backPopulateCaseLinkTable(List<CaseDetails> cases) {

        for (CaseDetails caseDetails : cases) {
            final CaseTypeDefinition caseTypeDefinition =
                caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());

            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());
        }
    }
}
