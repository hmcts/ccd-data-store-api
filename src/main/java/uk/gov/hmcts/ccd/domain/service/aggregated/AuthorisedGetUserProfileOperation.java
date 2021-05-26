package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Service
@Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER)
public class AuthorisedGetUserProfileOperation implements GetUserProfileOperation {
    public static final String QUALIFIER = "authorised";

    private final AccessControlService accessControlService;
    private final GetUserProfileOperation getUserProfileOperation;
    private final CaseDataAccessControl caseDataAccessControl;

    public AuthorisedGetUserProfileOperation(AccessControlService accessControlService,
                                             @Qualifier(DefaultGetUserProfileOperation.QUALIFIER)
                                                 GetUserProfileOperation getUserProfileOperation,
                                             CaseDataAccessControl caseDataAccessControl) {
        this.accessControlService = accessControlService;
        this.getUserProfileOperation = getUserProfileOperation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public UserProfile execute(Predicate<AccessControlList> access) {
        return filterCaseTypes(getUserProfileOperation.execute(access), access);
    }

    private UserProfile filterCaseTypes(UserProfile userProfile, Predicate<AccessControlList> access) {
        Arrays.stream(userProfile.getJurisdictions()).forEach(
            jurisdiction -> jurisdiction.setCaseTypeDefinitions(
                jurisdiction.getCaseTypeDefinitions()
                    .stream()
                    .map(caseType -> verifyAccess(caseType, getAccessProfiles(caseType.getId()), access))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList())
            )
        );
        return userProfile;
    }

    private Set<String> getAccessProfiles(String caseTypeId) {
        List<AccessProfile> accessProfiles = caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
        return caseDataAccessControl.extractAccessProfileNames(accessProfiles);
    }

    private Optional<CaseTypeDefinition> verifyAccess(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles,
                                                      Predicate<AccessControlList> access) {
        if (caseTypeDefinition == null || CollectionUtils.isEmpty(userRoles)
            || !accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, access)) {
            return Optional.empty();
        }
        caseTypeDefinition.setStates(accessControlService.filterCaseStatesByAccess(caseTypeDefinition,
            userRoles, access));
        caseTypeDefinition.setEvents(accessControlService.filterCaseEventsByAccess(caseTypeDefinition,
            userRoles, access));

        return Optional.of(caseTypeDefinition);
    }
}
