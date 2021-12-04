package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
@Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER)
public class AuthorisedGetCaseTypeOperation implements GetCaseTypeOperation {
    public static final String QUALIFIER = "authorised";
    private final AccessControlService accessControlService;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public AuthorisedGetCaseTypeOperation(final AccessControlService accessControlService,
                                          @Qualifier(DefaultGetCaseTypeOperation.QUALIFIER)
                                              final GetCaseTypeOperation getCaseTypeOperation,
                                          CaseDataAccessControl caseDataAccessControl) {
        this.accessControlService = accessControlService;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public Optional<CaseTypeDefinition> execute(String caseTypeId, Predicate<AccessControlList> access) {
        final Set<AccessProfile> userRoles = getAccessProfiles(caseTypeId);
        return getCaseTypeOperation.execute(caseTypeId, access)
            .flatMap(caseType -> verifyAccess(caseType, userRoles, access));
    }

    private Set<AccessProfile> getAccessProfiles(String caseTypeId) {
        Set<AccessProfile> accessProfiles = caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
        if (accessProfiles == null) {
            throw new ValidationException("Cannot find access profiles for the user");
        }

        return accessProfiles;
    }

    private Optional<CaseTypeDefinition> verifyAccess(CaseTypeDefinition caseTypeDefinition,
                                                      Set<AccessProfile> accessProfiles,
                                                      Predicate<AccessControlList> access) {

        if (CollectionUtils.isEmpty(accessProfiles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, access)) {
            return Optional.empty();
        }

        caseTypeDefinition.setStates(accessControlService.filterCaseStatesByAccess(caseTypeDefinition,
                                                                         accessProfiles,
                                                                         access));
        caseTypeDefinition.setEvents(accessControlService.filterCaseEventsByAccess(caseTypeDefinition,
            accessProfiles, access));

        caseTypeDefinition.setCaseFieldDefinitions(accessControlService.filterCaseFieldsByAccess(
                                                    caseTypeDefinition.getCaseFieldDefinitions(),
                                                    accessProfiles,
                                                    access));


        return Optional.of(caseTypeDefinition);
    }

}
