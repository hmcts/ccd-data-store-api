package uk.gov.hmcts.ccd.domain.service.validate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
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
        caseDefinitionRepository.getCaseType(caseTypeId)
            .findCaseEvent(content.getEventId())
            .ifPresent(caseEventDefinition -> caseEventDefinition.getCaseFields()
                .stream()
                .forEach(eventFieldDefinition -> eventFieldDefinition.getCaseEventFieldComplexDefinitions().stream()
                    .filter(cefcDefinition -> isOrgPolicyCaseAssignedRole(cefcDefinition.getReference()))
                    .forEach(cefcDefinition -> {
                        String reference = cefcDefinition.getReference();
                        validateContent(content, eventFieldDefinition.getCaseFieldId(),
                            reference,
                            cefcDefinition.getDefaultValue(),
                            errorList);
                    })));
        if (errorList.size() != 0) {
            throw new ValidationException("Roles validation error: " + String.join(", ", errorList));
        }
    }

    private String[] convertReference(String reference) {
        return reference.split(Pattern.quote("."));
    }

    private boolean isOrgPolicyCaseAssignedRole(String reference) {
        String[] referenceArray = convertReference(reference);
        return ORGANISATION_POLICY_ROLE.equals(referenceArray[referenceArray.length - 1]);
    }

    private void validateContent(final CaseDataContent content,
                                 final String caseFieldId,
                                 final String reference,
                                 final String defaultValue,
                                 final List<String> errorList) {
        final JsonNode existingData = new ObjectMapper().convertValue(content.getData(), JsonNode.class);
        JsonNode caseFieldNode = existingData.findPath(caseFieldId);
        if (caseFieldNode != null) {
            String[] referenceArray = convertReference(reference);
            int length = referenceArray.length;
            String nodeReference = length > 1 ? referenceArray[length - 2] : referenceArray[length - 1];
            List<JsonNode> parentNodes = caseFieldNode.findParents(nodeReference);
            parentNodes.stream().forEach(parentNode -> {
                JsonNode orgPolicyNode = findOrgPolicyNode(nodeReference, parentNode, length);
                if (orgPolicyNode.isNull()) {
                    errorList.add(caseFieldId + " cannot have an empty value.");
                } else if (!defaultValue.equalsIgnoreCase(orgPolicyNode.textValue())) {
                    errorList.add(caseFieldId + " has an incorrect value " + orgPolicyNode.textValue());
                }
            });
        }
    }

    private JsonNode findOrgPolicyNode(final String nodeReference,
                                       final JsonNode parentNode,
                                       final int length) {
        if (length > 1) {
            return parentNode
                .findPath(nodeReference)
                .findPath(ORGANISATION_POLICY_ROLE);
        }
        return parentNode.findPath(ORGANISATION_POLICY_ROLE);
    }

    private boolean hasEventId(CaseTypeDefinition caseTypeDefinition, String eventId) {
        return caseTypeDefinition.hasEventId(eventId);
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition) {
        caseTypeService.validateData(data, caseTypeDefinition);
    }
}
