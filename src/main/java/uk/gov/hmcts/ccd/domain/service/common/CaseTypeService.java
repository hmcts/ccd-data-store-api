package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.domain.model.std.CaseValidationError;
import uk.gov.hmcts.ccd.domain.types.CaseDataValidator;
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
public class CaseTypeService {
    private final CaseDataValidator caseDataValidator;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private static final Logger LOG = LoggerFactory.getLogger(CaseTypeService.class);

    @Inject
    public CaseTypeService(final CaseDataValidator caseDataValidator,
                           @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDataValidator = caseDataValidator;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public CaseState findState(CaseType caseType, String stateId) {
        Optional<CaseState> optionalState = caseType.getStates()
            .stream()
            .filter(state -> state.getId().equals(stateId))
            .findFirst();
        return optionalState.orElseThrow(() -> new //
            ResourceNotFoundException(String.format("No state found with id '%s' for case type '%s'",
                                                    stateId,
                                                    caseType.getId())));
    }

    public Boolean isJurisdictionValid(final String jurisdictionId,
                                       final CaseType caseType) {
        return null == caseType
               || null == jurisdictionId
               || caseType.getJurisdiction().getId().equalsIgnoreCase(jurisdictionId);
    }

    public void validateData(final Map<String, JsonNode> data,
                             final CaseType caseType) {
        final List<ValidationResult> dataValidationResults = caseDataValidator.validate(data, caseType.getCaseFields());
        if (!dataValidationResults.isEmpty()) {
            LOG.warn("There have been validation errors={}", dataValidationResults);
            final List<CaseFieldValidationError> fieldErrors = dataValidationResults.stream()
                .map(validationResult -> new CaseFieldValidationError(validationResult.getFieldId(), validationResult.getErrorMessage()))
                .collect(Collectors.toList());
            throw new CaseValidationException()
                .withDetails(new CaseValidationError(fieldErrors));
        }
    }

    public CaseType getCaseTypeForJurisdiction(final String caseTypeId,
                                               final String jurisdictionId) {
        final CaseType caseType = getCaseType(caseTypeId);

        if (null == jurisdictionId || !jurisdictionId.equalsIgnoreCase(caseType.getJurisdiction().getId())) {
            throw new ResourceNotFoundException(
                String.format(
                    "Case type with id %s could not be found for jurisdiction %s",
                    caseTypeId,
                    jurisdictionId
                )
            );
        }
        return caseType;
    }

    public CaseType getCaseType(String caseTypeId) {
        return ofNullable(caseDefinitionRepository.getCaseType(caseTypeId))
            .orElseThrow(() -> new ResourceNotFoundException(String.format("Case type with id %s could not be found", caseTypeId)));
    }

    public List<CaseType> getCaseTypesForJurisdiction(final String jurisdictionId) {
        final List<CaseType> caseTypes = caseDefinitionRepository.getCaseTypesForJurisdiction(jurisdictionId);

        if (null == caseTypes
            || null == jurisdictionId) {

            throw new ResourceNotFoundException(
                String.format(
                    "Case types could not be found for jurisdiction %s",
                    jurisdictionId
                )
            );
        }
        return caseTypes;
    }
}
