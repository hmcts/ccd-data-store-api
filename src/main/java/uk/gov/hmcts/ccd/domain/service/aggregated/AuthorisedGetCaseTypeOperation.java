package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER)
public class AuthorisedGetCaseTypeOperation implements GetCaseTypeOperation {
    public static final String QUALIFIER = "authorised";
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final GetCaseTypeOperation getCaseTypeOperation;

    @Autowired
    public AuthorisedGetCaseTypeOperation(final AccessControlService accessControlService,
                                          @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                          @Qualifier(DefaultGetCaseTypeOperation.QUALIFIER) final GetCaseTypeOperation getCaseTypeOperation) {
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.getCaseTypeOperation = getCaseTypeOperation;
    }

    @Override
    public Optional<CaseType> execute(String caseTypeId, Predicate<AccessControlList> access) {
        final Set<String> userRoles = getUserRoles();
        return getCaseTypeOperation.execute(caseTypeId, access)
            .flatMap(caseType -> verifyAccess(caseType, userRoles, access));
    }

    private Set<String> getUserRoles() {
        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        return userRoles;
    }

    private Optional<CaseType> verifyAccess(CaseType caseType, Set<String> userRoles, Predicate<AccessControlList> access) {

        if (CollectionUtils.isEmpty(userRoles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, access)) {
            return Optional.empty();
        }

        caseType.setStates(accessControlService.filterCaseStatesByAccess(caseType.getStates(),
                                                                         userRoles,
                                                                         access));
        caseType.setEvents(accessControlService.filterCaseEventsByAccess(caseType.getEvents(),
                                                                         userRoles,
                                                                         access));

        caseType.setCaseFields(accessControlService.filterCaseFieldsByAccess(caseType.getCaseFields(),
                                                                             userRoles,
                                                                             access));


        return Optional.of(caseType);
    }

}
