package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@Service
@Qualifier(AuthorisedFindWorkbasketInputOperation.QUALIFIER)
public class AuthorisedFindWorkbasketInputOperation implements FindWorkbasketInputOperation {

    public static final String QUALIFIER = "authorised";
    private final FindWorkbasketInputOperation findWorkbasketInputOperation;
    private final GetCaseTypeOperation getCaseTypeOperation;

    public AuthorisedFindWorkbasketInputOperation(@Qualifier(ClassifiedFindWorkbasketInputOperation.QUALIFIER) final FindWorkbasketInputOperation findWorkbasketInputOperation,
                                                  @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) final GetCaseTypeOperation getCaseTypeOperation) {
        this.findWorkbasketInputOperation = findWorkbasketInputOperation;
        this.getCaseTypeOperation = getCaseTypeOperation;
    }

    public List<WorkbasketInput> execute(final String caseTypeId, Predicate<AccessControlList> access) {
        Optional<CaseType> caseType = this.getCaseTypeOperation.execute(caseTypeId, access);

        if (!caseType.isPresent()) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }

        return findWorkbasketInputOperation.execute(caseTypeId, access).stream()
            .filter(workbasketInput -> caseType.get().getCaseFields()
                .stream()
                .anyMatch(caseField -> caseField.getId().equalsIgnoreCase(workbasketInput.getField().getId())))
            .collect(toList());
    }
}
