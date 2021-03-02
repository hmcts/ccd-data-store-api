package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.types.ValidationContext;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;
import java.util.Map;

@Service
public class DefaultValidateCaseFieldsOperation implements ValidateCaseFieldsOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseTypeService caseTypeService;
    private final FieldProcessorService fieldProcessorService;

    @Inject
    DefaultValidateCaseFieldsOperation(
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final CaseTypeService caseTypeService,
        final FieldProcessorService fieldProcessorService
    ) {

        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseTypeService = caseTypeService;
        this.fieldProcessorService = fieldProcessorService;
    }

    @Override
    public final Map<String, JsonNode> validateCaseDetails(String caseTypeId, CaseDataContent content) {
        if (content == null || content.getEvent() == null || content.getEventId() == null) {
            throw new ValidationException("Cannot validate case field because of event is not specified");
        }
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }
        if (!hasEventId(caseTypeDefinition, content.getEventId())) {
            throw new ValidationException("Cannot validate case field because of event " + content.getEventId()
                + " is not found in case type definition");
        }
        content.setData(fieldProcessorService.processData(content.getData(), caseTypeDefinition, content.getEventId()));
        caseTypeService.validateData(new ValidationContext(caseTypeDefinition, content.getData()));
        return content.getData();
    }

    private boolean hasEventId(CaseTypeDefinition caseTypeDefinition, String eventId) {
        return caseTypeDefinition.hasEventId(eventId);
    }

    @Override
    public void validateData(Map<String, JsonNode> data,
                             CaseTypeDefinition caseTypeDefinition,
                             final CaseDataContent content) {
        caseTypeService.validateData(new ValidationContext(caseTypeDefinition, data));
    }
}
