package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        final Optional<String> defaultValue = getOrganisationPolicyDefaultRoleValue(content);
        // if there is not a default value. it means that there will not be organisation policy validation.
        if (!defaultValue.isPresent()) {
            return;
        }

        caseDefinitionRepository.getCaseType(caseTypeId).getEvents().stream().filter(
            event -> event.getId().equals(content.getEventId())
        ).forEach(
            caseEventDefinition ->  caseEventDefinition.getCaseFields().stream().forEach(
                caseField -> caseField.getCaseEventFieldComplexDefinitions().stream().filter(
                    caseEventFieldComplexDefinition -> {
                        if (caseEventFieldComplexDefinition.getReference().equals(ORGANISATION_POLICY_ROLE)) {
                            return validateOrgPolicyCaseAssignedRole(caseEventFieldComplexDefinition, defaultValue.get());
                        } else {
                            return false;
                        }
                    }
                ).collect(Collectors.toList()))
        );
    }

    private Optional<String> getOrganisationPolicyDefaultRoleValue(final CaseDataContent content) {

        final JsonNode existingDat  = new ObjectMapper().convertValue(content.getData(), JsonNode.class);
        final List<JsonNode> jsonNode = existingDat.findParents(ORGANISATION_POLICY_ROLE);
        final  Optional<JsonNode> node = jsonNode.stream().findFirst();

        if (node.isPresent()) {
            return Optional.of(node.get().get(ORGANISATION_POLICY_ROLE).textValue());
        }
        return Optional.ofNullable(null);
    }

    private  boolean validateOrgPolicyCaseAssignedRole(CaseEventFieldComplexDefinition caseEventFieldComplexDefinition, String defaultValue) {
        if (!caseEventFieldComplexDefinition.getDefaultValue().equals(defaultValue)) {
            throw new ValidationException(ORGANISATION_POLICY_ROLE + " filed has an incorrect value.");
        }
        return false;
    }


    private boolean hasEventId(CaseTypeDefinition caseTypeDefinition, String eventId) {
        return caseTypeDefinition.hasEventId(eventId);
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition) {
        caseTypeService.validateData(data, caseTypeDefinition);
    }
}
