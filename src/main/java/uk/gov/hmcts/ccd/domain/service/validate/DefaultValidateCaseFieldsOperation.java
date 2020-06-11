package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
public class DefaultValidateCaseFieldsOperation implements ValidateCaseFieldsOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseTypeService caseTypeService;
    private final FieldProcessorService fieldProcessorService;
    public static final String ORGANISATION_POLICY_ROLE = "OrgPolicyCaseAssignedRole";

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
            throw new ValidationException("Cannot validate case field because of event " + content.getEventId() + " is not found in case type definition");
        }
        content.setData(fieldProcessorService.processData(content.getData(), caseTypeDefinition, content.getEventId()));
        caseTypeService.validateData(content.getData(), caseTypeDefinition);

        validateOrganisationPolicy(caseTypeId, content);
        return content.getData();
    }

    private void validateOrganisationPolicy(String caseTypeId, CaseDataContent content) {
        final List<String> errorList = new ArrayList<>();
        caseDefinitionRepository.getCaseType(caseTypeId).getEvents().stream()
            .filter(event -> event.getId().equals(content.getEventId()))
            .forEach(caseEventDefinition -> caseEventDefinition.getCaseFields()
                .stream()
                .forEach(caseField -> caseField.getCaseEventFieldComplexDefinitions().stream()
                    .filter(cefcDefinition -> isOrgPolicyCaseAssignedRole(cefcDefinition.getReference()))
                    .forEach(cefcDefinition -> {
                        //get extract the default value  from the content for the current caseField
                        String reference = cefcDefinition.getReference();
                        final Optional<String> caseFieldDefaultValue =
                            getDefaultValueFromContentByCaseFieldID(content, caseField.getCaseFieldId(), reference);
                        validateOrgPolicyCaseAssignedRole(
                            cefcDefinition,
                            caseFieldDefaultValue,
                            caseField.getCaseFieldId(), errorList);
                    })));
        if (errorList.size() != 0) {
            throw new ValidationException("Roles validation error: " + String.join(", ", errorList));
        }
    }

    private boolean isOrgPolicyCaseAssignedRole(String reference) {
        String[] referenceArray = reference.split(Pattern.quote("."));
        return ORGANISATION_POLICY_ROLE.equals(referenceArray[referenceArray.length -1]);
    }

    private Optional<String> getDefaultValueFromContentByCaseFieldID(final CaseDataContent content,
                                                                     final String caseFiledID,
                                                                     final String reference) {
        final JsonNode existingData = new ObjectMapper().convertValue(content.getData(), JsonNode.class);
        final Optional<JsonNode> caseFieldNode = Optional.ofNullable(existingData.get(caseFiledID));

        if (caseFieldNode.isPresent() && !caseFieldNode.get().get(reference).isNull())  {
            return Optional.of(caseFieldNode.get().get(reference).textValue());
        }
        return Optional.ofNullable(null);
    }

    private void validateOrgPolicyCaseAssignedRole(final CaseEventFieldComplexDefinition caseEventFieldComplexDefinition,
                                                   final Optional<String> defaultValue,
                                                   final String caseFiledID,
                                                   final List<String> errorList) {
        if (!defaultValue.isPresent()) {
            errorList.add(caseFiledID + " cannot have an empty value.");
        } else if (!caseEventFieldComplexDefinition.getDefaultValue().equals(defaultValue.get())) {
            errorList.add(caseFiledID + " has an incorrect value.");
        }
    }


    private boolean hasEventId(CaseTypeDefinition caseTypeDefinition, String eventId) {
        return caseTypeDefinition.hasEventId(eventId);
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition) {
        caseTypeService.validateData(data, caseTypeDefinition);
    }
}
