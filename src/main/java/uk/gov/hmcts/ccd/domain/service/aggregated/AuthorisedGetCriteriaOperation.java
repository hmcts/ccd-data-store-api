package uk.gov.hmcts.ccd.domain.service.aggregated;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER)
public class AuthorisedGetCriteriaOperation implements GetCriteriaOperation {

    public static final String QUALIFIER = "authorised";
    private final GetCriteriaOperation getCriteriaOperation;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final UserRepository userRepository;

    public AuthorisedGetCriteriaOperation(
        @Qualifier(ClassifiedGetCriteriaOperation.QUALIFIER) final GetCriteriaOperation getCriteriaOperation,
        @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) final GetCaseTypeOperation getCaseTypeOperation,
        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.getCriteriaOperation = getCriteriaOperation;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.userRepository = userRepository;
    }

    public <T> List<? extends CriteriaInput> execute(final String caseTypeId, Predicate<AccessControlList> access, CriteriaType criteriaType) {
        Optional<CaseTypeDefinition> caseType = this.getCaseTypeOperation.execute(caseTypeId, access);

        if (!caseType.isPresent()) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }
        final HashSet<String> addedFields = new HashSet<>();
        return getCriteriaOperation.execute(caseTypeId, access, criteriaType).stream()
            .filter(crInput -> criteriaAllowedByCRUD(caseType.get(), crInput))
            .filter(input -> filterDistinctFieldsByRole(addedFields, input, userRepository.getUserRoles()))
            .collect(Collectors.toList());
    }

    private boolean criteriaAllowedByCRUD(CaseTypeDefinition caseTypeDefinition, CriteriaInput criteriaInput) {
        return caseTypeDefinition
            .getCaseFieldDefinitions()
            .stream()
            .anyMatch(caseField -> caseField.getId().equalsIgnoreCase(criteriaInput.getField().getId()));
    }

    private boolean filterDistinctFieldsByRole(final Set<String> addedFields, final CriteriaInput criteriaInput, final Set<String> userRoles) {
        String id = criteriaInput.buildCaseFieldId();
        if (addedFields.contains(id)) {
            return false;
        } else {
            if (StringUtils.isEmpty(criteriaInput.getRole()) || userRoles.contains(criteriaInput.getRole())) {
                addedFields.add(id);
                return true;
            } else {
                return false;
            }
        }
    }

}
