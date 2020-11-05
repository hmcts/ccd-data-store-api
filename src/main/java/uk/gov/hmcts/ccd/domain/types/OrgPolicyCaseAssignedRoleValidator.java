package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Named
@Singleton
public class OrgPolicyCaseAssignedRoleValidator implements FieldIdBasedValidator {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseRoleRepository caseRoleRepository;
    private ValidationContext validationContext;

    @Inject
    public OrgPolicyCaseAssignedRoleValidator(
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        @Qualifier(CachedCaseRoleRepository.QUALIFIER) final CaseRoleRepository caseRoleRepository
    ) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseRoleRepository = caseRoleRepository;
    }

    @Override
    public String getFieldId() {
        return PredefinedFieldsIDs.ORG_POLICY_CASE_ASSIGNED_ROLE.getId();
    }

    @Override
    public List<ValidationResult> validate(String dataFieldId, JsonNode dataValue, CaseFieldDefinition caseFieldDefinition) {
        caseFieldDefinition.getFieldTypeDefinition();
        return validateOrganisationPolicy(dataValue, caseFieldDefinition);
    }

    @Override
    public void setValidationContext(ValidationContext validationContext) {
        this.validationContext = validationContext;
    }

    private List<ValidationResult> validateOrganisationPolicy(JsonNode dataValue, CaseFieldDefinition caseFieldDefinition) {
        final String caseTypeId = validationContext.getCaseTypeId();
        final Set<String> caseRoles = caseRoleRepository.getCaseRoles(caseTypeId);
        final List<ValidationResult> errors = new ArrayList<>();
        validateContent(caseFieldDefinition.getId(), dataValue, caseRoles, errors);
        if (errors.isEmpty()) {
            return Collections.emptyList();
        }
        return errors;
    }

    private void validateContent(final String caseFieldId, final JsonNode orgPolicyRoleNode, final Set<String> caseRoles, final List<ValidationResult> errors) {

        if (orgPolicyRoleNode.isNull()) {
            errors.add(new ValidationResult(validationContext.getPath() + " organisation role cannot have an empty value.", caseFieldId));
        } else if (!caseRolesContainsCaseInsensitive(caseRoles, orgPolicyRoleNode)) {
            errors.add(new ValidationResult(validationContext.getPath() + " The value  " + orgPolicyRoleNode.textValue() + " is not a valid organisation role.", caseFieldId));
        }
    }

    private boolean caseRolesContainsCaseInsensitive(Set<String> caseRoles, JsonNode orgPolicyRoleNode) {
        return caseRoles.stream().anyMatch(e -> e.equalsIgnoreCase(orgPolicyRoleNode.textValue()));
    }
}
