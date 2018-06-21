package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractDefaultGetCaseViewOperation {

    public static final String QUALIFIER = "default";
    private static final String RESOURCE_NOT_FOUND //
        = "No case found ( jurisdiction = '%s', case type = '%s', case reference = '%s' )";

    private final GetCaseOperation getCaseOperation;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseTypeService caseTypeService;
    private final UIDService uidService;

    AbstractDefaultGetCaseViewOperation(GetCaseOperation getCaseOperation,
                                        UIDefinitionRepository uiDefinitionRepository,
                                        CaseTypeService caseTypeService,
                                        UIDService uidService) {
        this.getCaseOperation = getCaseOperation;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseTypeService = caseTypeService;
        this.uidService = uidService;
    }

    void validateCaseReference(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference " + caseReference + " is not valid");
        }
    }

    CaseType getCaseType(String jurisdictionId, String caseTypeId) {
        return caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
    }

    CaseDetails getCaseDetails(String jurisdictionId, String caseTypeId, String caseReference) {
        return getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference).orElseThrow(
            () -> new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND,
                                                              jurisdictionId,
                                                              caseTypeId,
                                                              caseReference)));
    }

    CaseTabCollection getCaseTabCollection(String caseTypeId) {
        return uiDefinitionRepository.getCaseTabCollection(caseTypeId);
    }

    private Predicate<CaseTypeTabField> filterCaseTabFieldsBasedOnSecureData(CaseDetails caseDetails) {
        return caseDetails::existsInData;
    }

    CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, JsonNode> data) {
        return getTabs(caseDetails, data, getCaseTabCollection(caseDetails.getCaseTypeId()));
    }

    CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, JsonNode> data, CaseTabCollection caseTabCollection) {
        return caseTabCollection.getTabs().stream().map(tab -> {
            CaseViewField[] caseViewFields = tab.getTabFields().stream()
                .filter(filterCaseTabFieldsBasedOnSecureData(caseDetails))
                .map(field -> CaseViewField.createFrom(field, data))
                .toArray(CaseViewField[]::new);

            return new CaseViewTab(tab.getId(), tab.getLabel(), tab.getDisplayOrder(), caseViewFields,
                                   tab.getShowCondition());

        }).toArray(CaseViewTab[]::new);
    }

}
