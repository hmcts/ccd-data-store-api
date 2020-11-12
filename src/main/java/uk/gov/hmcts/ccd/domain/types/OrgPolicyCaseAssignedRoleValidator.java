package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;

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

    private final CaseRoleRepository caseRoleRepository;

    @Inject
    public OrgPolicyCaseAssignedRoleValidator(
        @Qualifier(CachedCaseRoleRepository.QUALIFIER) final CaseRoleRepository caseRoleRepository
    ) {
        this.caseRoleRepository = caseRoleRepository;
    }

    @Override
    public String getFieldId() {
        return PredefinedFieldsIDs.ORG_POLICY_CASE_ASSIGNED_ROLE.getId();
    }

    @Override
    public List<ValidationResult> validate(ValidationContext validationContext) {
        return validateOrganisationPolicy(validationContext);
    }

    private List<ValidationResult> validateOrganisationPolicy(ValidationContext validationContext) {
        final String caseTypeId = validationContext.getCaseTypeId();
        final Set<String> caseRoles = caseRoleRepository.getCaseRoles(caseTypeId);
        final List<ValidationResult> errors = new ArrayList<>();
        validateContent(validationContext, caseRoles, errors);
        if (errors.isEmpty()) {
            return Collections.emptyList();
        }
        return errors;
    }

    private void validateContent(ValidationContext validationContext, final Set<String> caseRoles,
                                 final List<ValidationResult> errors) {

        final JsonNode orgPolicyRoleNode = validationContext.getFieldValue();
        if (orgPolicyRoleNode.isNull()) {
            errors.add(new ValidationResult(
                String.format("%s Organisation role cannot have an empty value.", validationContext.getPath()),
                validationContext.getFieldId())
            );
        } else if (!caseRolesContainsCaseInsensitive(caseRoles, orgPolicyRoleNode)) {
            final String error = String.format("%1$s The value %2$s is not a valid organisation role.",
                validationContext.getPath(),
                orgPolicyRoleNode.textValue()
            );
            errors.add(new ValidationResult(error, validationContext.getFieldId()));
        }
    }

    private boolean caseRolesContainsCaseInsensitive(Set<String> caseRoles, JsonNode orgPolicyRoleNode) {
        return caseRoles.stream().anyMatch(e -> e.equalsIgnoreCase(orgPolicyRoleNode.textValue()));
    }
}
