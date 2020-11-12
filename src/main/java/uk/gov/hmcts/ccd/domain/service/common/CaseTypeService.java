package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.domain.types.CaseDataValidator;
import uk.gov.hmcts.ccd.domain.types.ValidationContext;
import uk.gov.hmcts.ccd.domain.types.ValidationResult;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseValidationException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Named
@Singleton
@SuppressWarnings("checkstyle:SummaryJavadoc")
// partial javadoc attributes added prior to checkstyle implementation in module
public class CaseTypeService {
    private final CaseDataValidator caseDataValidator;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Inject
    public CaseTypeService(final CaseDataValidator caseDataValidator,
                           @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDataValidator = caseDataValidator;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public CaseStateDefinition findState(CaseTypeDefinition caseTypeDefinition, String stateId) {
        Optional<CaseStateDefinition> optionalState = caseTypeDefinition.getStates()
            .stream()
            .filter(state -> state.getId().equals(stateId))
            .findFirst();
        return optionalState.orElseThrow(() -> new //
            ResourceNotFoundException(String.format("No state found with id '%s' for case type '%s'",
                                                    stateId,
                                                    caseTypeDefinition.getId())));
    }

    public Boolean isJurisdictionValid(final String jurisdictionId,
                                       final CaseTypeDefinition caseTypeDefinition) {
        return null == caseTypeDefinition
               || null == jurisdictionId
               || caseTypeDefinition.getJurisdictionDefinition().getId().equalsIgnoreCase(jurisdictionId);
    }

    public void validateData(final ValidationContext validationContext) {
        final List<ValidationResult> dataValidationResults = caseDataValidator.validate(validationContext);
        if (!dataValidationResults.isEmpty()) {
            final List<CaseFieldValidationError> fieldErrors = dataValidationResults.stream()
                .map(validationResult ->
                    new CaseFieldValidationError(validationResult.getFieldId(), validationResult.getErrorMessage()))
                .collect(Collectors.toList());
            throw new CaseValidationException(fieldErrors);
        }
    }

    public void validateData(final Map<String, JsonNode> data, final CaseTypeDefinition caseTypeDefinition) {
        validateData(new ValidationContext(caseTypeDefinition, data));
    }

    public CaseTypeDefinition getCaseTypeForJurisdiction(final String caseTypeId,
                                                         final String jurisdictionId) {
        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);

        if (null == jurisdictionId
            || !jurisdictionId.equalsIgnoreCase(caseTypeDefinition.getJurisdictionDefinition().getId())) {
            throw new ResourceNotFoundException(
                String.format(
                    "Case type with id %s could not be found for jurisdiction %s",
                    caseTypeId,
                    jurisdictionId
                )
            );
        }
        return caseTypeDefinition;
    }

    public CaseTypeDefinition getCaseType(String caseTypeId) {
        return ofNullable(caseDefinitionRepository.getCaseType(caseTypeId))
            .orElseThrow(() ->
                new ResourceNotFoundException(String.format("Case type with id %s could not be found", caseTypeId)));
    }

    /**
     *
     * @deprecated current implementation has serious performance issues
     */
    @Deprecated
    @SuppressWarnings("squid:S1133")
    public List<CaseTypeDefinition> getCaseTypesForJurisdiction(final String jurisdictionId) {
        final List<CaseTypeDefinition> caseTypeDefinitions =
            caseDefinitionRepository.getCaseTypesForJurisdiction(jurisdictionId);

        if (null == caseTypeDefinitions
            || null == jurisdictionId) {

            throw new ResourceNotFoundException(
                String.format(
                    "Case types could not be found for jurisdiction %s",
                    jurisdictionId
                )
            );
        }
        return caseTypeDefinitions;
    }
}
