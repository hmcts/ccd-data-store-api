package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import java.util.Date;
import java.util.List;

import static uk.gov.hmcts.ccd.v2.V2.Error.CASE_DATA_NOT_FOUND;

@Service
@Qualifier("authorised")
public class AuthorisedClosedCaseSearchOperationImpl implements ClosedCaseSearchOperation {

    private final ClosedCaseSearchOperation closedCaseSearchOperation;
    private final GetCaseOperation getCaseOperation;

    public AuthorisedClosedCaseSearchOperationImpl(
        @Qualifier(DefaultClosedCaseSearchOperation.QUALIFIER) ClosedCaseSearchOperation closedCaseSearchOperation,
        @Qualifier("authorised") GetCaseOperation getCaseOperation) {
        this.closedCaseSearchOperation = closedCaseSearchOperation;
        this.getCaseOperation = getCaseOperation;
    }

    @Override
    @Transactional
    public DateCaseClosedResponse execute(Date closedCaseDate) {
        List<String> closedCaseReferences = closedCaseSearchOperation.execute(closedCaseDate).getCaseReferences();

        if (closedCaseReferences == null || closedCaseReferences.isEmpty()) {
            throw new CaseNotFoundException(CASE_DATA_NOT_FOUND, null);
        }

        List<String> caseReferences = closedCaseReferences.stream()
            .filter(this::hasReadAccess)
            .toList();

        if (caseReferences.isEmpty()) {
            throw new CaseNotFoundException(CASE_DATA_NOT_FOUND, null);
        }

        return new DateCaseClosedResponse(caseReferences);
    }

    private boolean hasReadAccess(String caseReference) {
        return getCaseOperation.execute(caseReference).isPresent();
    }
}
