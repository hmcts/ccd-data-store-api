package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

/**
 * Authorised {@link GetCaseTypesOperation}.
 *
 * @deprecated until {@link DefaultGetCaseTypesOperation} is deprecated
 */
@Deprecated
@SuppressWarnings("squid:S1133")
@Service
@Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER)
public class AuthorisedGetCaseTypesOperation implements GetCaseTypesOperation {
    public static final String QUALIFIER = "authorised";
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final GetCaseTypesOperation getCaseTypesOperation;

    @Autowired
    public AuthorisedGetCaseTypesOperation(final AccessControlService accessControlService,
                                           @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                           @Qualifier(DefaultGetCaseTypesOperation.QUALIFIER) final GetCaseTypesOperation getCaseTypesOperation) {
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.getCaseTypesOperation = getCaseTypesOperation;
    }

    @Override
    public List<CaseTypeDefinition> execute(String jurisdictionId, Predicate<AccessControlList> access) {
        final Set<String> userRoles = getUserRoles();
        return getCaseTypesOperation.execute(jurisdictionId, access).stream()
            .map(caseType -> verifyAccess(caseType, userRoles, access))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Set<String> getUserRoles() {
        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        return userRoles;
    }

    private Optional<CaseTypeDefinition> verifyAccess(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles, Predicate<AccessControlList> access) {

        if (caseTypeDefinition == null || CollectionUtils.isEmpty(userRoles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, access)) {
            return Optional.empty();
        }

        caseTypeDefinition.setStates(accessControlService.filterCaseStatesByAccess(caseTypeDefinition.getStates(),
                                                                         userRoles,
                                                                         access));
        caseTypeDefinition.setEvents(accessControlService.filterCaseEventsByAccess(caseTypeDefinition.getEvents(),
                                                                         userRoles,
                                                                         access));

        caseTypeDefinition.setCaseFieldDefinitions(accessControlService.filterCaseFieldsByAccess(caseTypeDefinition.getCaseFieldDefinitions(),
                                                                             userRoles,
                                                                             access));


        return Optional.of(caseTypeDefinition);
    }

}
