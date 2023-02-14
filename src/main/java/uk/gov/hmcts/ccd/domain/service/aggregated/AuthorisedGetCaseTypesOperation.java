package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private final GetCaseTypesOperation getCaseTypesOperation;
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public AuthorisedGetCaseTypesOperation(final AccessControlService accessControlService,
                                           @Qualifier(DefaultGetCaseTypesOperation.QUALIFIER)
                                               final GetCaseTypesOperation getCaseTypesOperation,
                                           CaseDataAccessControl caseDataAccessControl) {
        this.accessControlService = accessControlService;
        this.getCaseTypesOperation = getCaseTypesOperation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public List<CaseTypeDefinition> execute(String jurisdictionId, Predicate<AccessControlList> access) {
        return getCaseTypesOperation.execute(jurisdictionId, access).stream()
            .map(caseType -> verifyAccess(caseType, getAccessProfiles(caseType.getId()), access))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Set<AccessProfile> getAccessProfiles(String caseTypeId) {
        Set<AccessProfile> accessProfiles = caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
        if (accessProfiles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        return accessProfiles;
    }

    private Optional<CaseTypeDefinition> verifyAccess(CaseTypeDefinition caseTypeDefinition,
                                                      Set<AccessProfile> accessProfiles,
                                                      Predicate<AccessControlList> access) {

        if (caseTypeDefinition == null || CollectionUtils.isEmpty(accessProfiles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, access)) {
            return Optional.empty();
        }

        List<CaseEventDefinition> caseEventDefinitions = accessControlService
            .filterCaseEventsByAccess(caseTypeDefinition, accessProfiles, access);
        List<CaseStateDefinition> caseStateDefinitions = accessControlService
            .filterCaseStatesByAccess(caseTypeDefinition, accessProfiles, access);
        List<CaseFieldDefinition> caseFieldDefinitions = accessControlService.filterCaseFieldsByAccess(
            caseTypeDefinition.getCaseFieldDefinitions(),
            accessProfiles,
            access);

        return Optional.of(CaseTypeDefinition.caseTypeDefinitionCopy(caseTypeDefinition,
            caseEventDefinitions, caseStateDefinitions, caseFieldDefinitions).build());
    }

}
