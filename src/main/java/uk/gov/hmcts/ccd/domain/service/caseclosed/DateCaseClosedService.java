package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.Arrays;

@Service
public class DateCaseClosedService {

    private static final String CLOSED_FOR_PAYMENT = "CLOSED FOR PAYMENT";

    private final DateCaseClosedRepository dateCaseClosedRepository;
    private final CaseTypeService caseTypeService;

    public DateCaseClosedService(DateCaseClosedRepository dateCaseClosedRepository,
                                 CaseTypeService caseTypeService) {
        this.dateCaseClosedRepository = dateCaseClosedRepository;
        this.caseTypeService = caseTypeService;
    }

    public void updateForNewCase(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {
        String stateCategory = getStateCategory(caseDetails, caseTypeDefinition);

        if (hasClosedForPaymentCategory(stateCategory)) {
            saveDateCaseClosed(caseDetails, stateCategory);
        }
    }

    public void updateForExistingCase(CaseDetails caseDetails,
                                      CaseDetails caseDetailsBefore,
                                      CaseTypeDefinition caseTypeDefinition) {
        String stateCategory = getStateCategory(caseDetails, caseTypeDefinition);
        String previousStateCategory = getStateCategory(caseDetailsBefore, caseTypeDefinition);

        if (hasClosedForPaymentCategory(stateCategory)) {
            saveDateCaseClosed(caseDetails, stateCategory);
        } else if (hasClosedForPaymentCategory(previousStateCategory)) {
            dateCaseClosedRepository.deleteByCcdCaseNumber(caseDetails.getReference());
        }
    }

    private String getStateCategory(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {
        return caseTypeService.findState(caseTypeDefinition, caseDetails.getState()).getStateCategory();
    }

    private void saveDateCaseClosed(CaseDetails caseDetails, String stateCategory) {
        DateCaseClosedEntity dateCaseClosedEntity = dateCaseClosedRepository
            .findByCcdCaseNumber(caseDetails.getReference())
            .orElseGet(DateCaseClosedEntity::new);

        dateCaseClosedEntity.setCcdCaseNumber(caseDetails.getReference());
        dateCaseClosedEntity.setState(caseDetails.getState());
        dateCaseClosedEntity.setStateCategory(stateCategory);
        dateCaseClosedEntity.setStateChangedDate(caseDetails.getLastStateModifiedDate());
        dateCaseClosedRepository.save(dateCaseClosedEntity);
    }

    private boolean hasClosedForPaymentCategory(String stateCategory) {
        return stateCategory != null
               && Arrays.stream(stateCategory.split(","))
                   .map(String::trim)
                   .anyMatch(CLOSED_FOR_PAYMENT::equalsIgnoreCase);
    }
}
