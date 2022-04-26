package uk.gov.hmcts.ccd.domain.service.caselinking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.List;

@Slf4j
@Service
public class CaseLinkMigrationService {

    private final CaseLinkService caseLinkService;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public CaseLinkMigrationService(CaseLinkService caseLinkService,
                                    @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                    CaseDefinitionRepository caseDefinitionRepository) {
        this.caseLinkService = caseLinkService;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public void backPopulateCaseLinkTable(List<CaseDetails> cases) {

        for (CaseDetails caseDetails : cases) {
            final CaseTypeDefinition caseTypeDefinition =
                caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());

            caseLinkService.updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());
            log.info(
                "Populated case link table for case with id {} and reference {}",
                caseDetails.getId(), caseDetails.getReferenceAsString());
        }
    }

}
