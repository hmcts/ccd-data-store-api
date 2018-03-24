package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;
import java.util.Map;

@Service
public class DefaultValidateCaseFieldsOperation implements ValidateCaseFieldsOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseTypeService caseTypeService;

    @Inject DefaultValidateCaseFieldsOperation(
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final CaseTypeService caseTypeService) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseTypeService = caseTypeService;
    }

    @Override
    public final Map<String, JsonNode> validateCaseDetails(String jurisdictionId, String caseTypeId, Event event, Map<String, JsonNode> data) {
        if (event == null || event.getEventId() == null) {
            throw new ValidationException("Cannot validate case field because of event is not specified");
        }
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        if (!caseTypeService.isJurisdictionValid(jurisdictionId, caseType)) {
            throw new ValidationException("Cannot validate case field because of " + caseTypeId + " is not defined as case type for " + jurisdictionId);
        }
        caseTypeService.validateData(data, caseType);
        return data;
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseType caseType) {
        caseTypeService.validateData(data, caseType);
    }
}
