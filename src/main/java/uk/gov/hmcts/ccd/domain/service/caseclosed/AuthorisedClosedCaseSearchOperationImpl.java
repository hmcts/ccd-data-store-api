package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.v2.V2.Error.CASE_DATA_NOT_FOUND;

@Service
@Qualifier("authorised")
public class AuthorisedClosedCaseSearchOperationImpl implements ClosedCaseSearchOperation {

    public static final String DISPOSER_PAYMENT_USER_ROLE = "disposer-payment-user";

    private final ClosedCaseSearchOperation closedCaseSearchOperation;
    private final UserRepository userRepository;

    public AuthorisedClosedCaseSearchOperationImpl(
        @Qualifier(DefaultClosedCaseSearchOperation.QUALIFIER) ClosedCaseSearchOperation closedCaseSearchOperation,
        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.closedCaseSearchOperation = closedCaseSearchOperation;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DateCaseClosedResponse execute(LocalDate closedCaseDate) {
        if (!userRepository.anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE)) {
            throw new ForbiddenException();
        }

        List<String> closedCaseReferences = closedCaseSearchOperation.execute(closedCaseDate).getCaseReferences();

        if (closedCaseReferences == null || closedCaseReferences.isEmpty()) {
            throw new CaseNotFoundException(CASE_DATA_NOT_FOUND, null);
        }

        return new DateCaseClosedResponse(closedCaseReferences);
    }
}
