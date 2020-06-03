package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.CASE_HISTORY_VIEWER;

public abstract class AbstractDefaultGetCaseViewOperation {

    public static final String QUALIFIER = "default";

    private final GetCaseOperation getCaseOperation;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseTypeService caseTypeService;
    private final UIDService uidService;
    private final ObjectMapperService objectMapperService;
    private final CompoundFieldOrderService compoundFieldOrderService;
    private final FieldProcessorService fieldProcessorService;

    AbstractDefaultGetCaseViewOperation(GetCaseOperation getCaseOperation,
                                        UIDefinitionRepository uiDefinitionRepository,
                                        CaseTypeService caseTypeService,
                                        UIDService uidService,
                                        ObjectMapperService objectMapperService,
                                        CompoundFieldOrderService compoundFieldOrderService,
                                        FieldProcessorService fieldProcessorService) {
        this.getCaseOperation = getCaseOperation;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseTypeService = caseTypeService;
        this.uidService = uidService;
        this.objectMapperService = objectMapperService;
        this.compoundFieldOrderService = compoundFieldOrderService;
        this.fieldProcessorService = fieldProcessorService;
    }

    void validateCaseReference(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference " + caseReference + " is not valid");
        }
    }

    CaseType getCaseType(String caseTypeId) {
        return caseTypeService.getCaseType(caseTypeId);
    }

    CaseType getCaseType(String jurisdictionId, String caseTypeId) {
        return caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
    }

    CaseDetails getCaseDetails(String caseReference) {
        return getCaseOperation.execute(caseReference)
                               .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, ?> data) {
        return getTabs(caseDetails, data, getCaseTabCollection(caseDetails.getCaseTypeId()));
    }

    CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, ?> data, CaseTabCollection caseTabCollection) {
        return caseTabCollection.getTabs().stream().map(tab -> {
            CommonField[] caseViewFields = tab.getTabFields().stream()
                .filter(filterCaseTabFieldsBasedOnSecureData(caseDetails))
                .map(caseTypeTabField -> CaseViewField.createFrom(caseTypeTabField, data))
                .map(fieldProcessorService::processCaseViewField)
                .toArray(CaseViewField[]::new);
            return new CaseViewTab(tab.getId(), tab.getLabel(), tab.getDisplayOrder(), (CaseViewField[])caseViewFields,
                                   tab.getShowCondition(), tab.getRole());

        }).toArray(CaseViewTab[]::new);
    }

    CaseTabCollection getCaseTabCollection(String caseTypeId) {
        return uiDefinitionRepository.getCaseTabCollection(caseTypeId);
    }

    private Predicate<CaseTypeTabField> filterCaseTabFieldsBasedOnSecureData(CaseDetails caseDetails) {
        return caseDetails::existsInData;
    }

    List<CaseViewField> getMetadataFields(CaseType caseType, CaseDetails caseDetails) {
        return caseType.getCaseFields().stream()
            .filter(CaseField::isMetadata)
            .map(caseField -> CaseViewField.createFrom(caseField, caseDetails.getCaseDataAndMetadata()))
            .collect(Collectors.toList());
    }

    protected void hydrateHistoryField(CaseDetails caseDetails, CaseType caseType, List<CaseViewEvent> events) {
        for (CaseField caseField : caseType.getCaseFields()) {
            if (caseField.getFieldType().getType().equals(CASE_HISTORY_VIEWER)) {
                JsonNode eventsNode = objectMapperService.convertObjectToJsonNode(events);
                caseDetails.getData().put(caseField.getId(), eventsNode);
                return;
            }
        }
    }
}
