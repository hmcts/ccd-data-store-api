package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

@Service
@Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER)
public class AuthorisedGetCriteriaOperation implements GetCriteriaOperation {

    public static final String QUALIFIER = "authorised";
    private final GetCriteriaOperation getCriteriaOperation;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final CaseDataAccessControl caseDataAccessControl;

    public AuthorisedGetCriteriaOperation(
        @Qualifier(ClassifiedGetCriteriaOperation.QUALIFIER) final GetCriteriaOperation getCriteriaOperation,
        @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) final GetCaseTypeOperation getCaseTypeOperation,
        CaseDataAccessControl caseDataAccessControl) {
        this.getCriteriaOperation = getCriteriaOperation;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    public <T> List<? extends CriteriaInput> execute(final String caseTypeId,
                                                     Predicate<AccessControlList> access,
                                                     CriteriaType criteriaType) {
        Optional<CaseTypeDefinition> caseType = this.getCaseTypeOperation.execute(caseTypeId, access);

        if (!caseType.isPresent()) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }
        final HashSet<String> addedFields = new HashSet<>();
        Set<String> accessProfileNames = AccessControlService.extractAccessProfileNames(getAccessProfiles(caseTypeId));
        return getCriteriaOperation.execute(caseTypeId, access, criteriaType).stream()
            .filter(crInput -> criteriaAllowedByCRUD(caseType.get(), crInput))
            .filter(input -> filterDistinctFieldsByRole(addedFields,
                input,
                accessProfileNames))
            .collect(Collectors.toList());
    }

    private Set<AccessProfile> getAccessProfiles(String caseTypeId) {
        return caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
    }

    private boolean criteriaAllowedByCRUD(CaseTypeDefinition caseTypeDefinition, CriteriaInput criteriaInput) {
        return caseTypeDefinition
            .getCaseFieldDefinitions()
            .stream()
            .anyMatch(caseField -> caseField.getId().equalsIgnoreCase(criteriaInput.getField().getId()));
    }

    private boolean filterDistinctFieldsByRole(final Set<String> addedFields,
                                               final CriteriaInput criteriaInput,
                                               final Set<String> accessProfiles) {
        String id = criteriaInput.buildCaseFieldId();
        if (addedFields.contains(id)) {
            return false;
        } else {
            if (StringUtils.isEmpty(criteriaInput.getRole()) || accessProfiles.contains(criteriaInput.getRole())) {
                addedFields.add(id);
                return true;
            } else {
                return false;
            }
        }
    }

}
