package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Set;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseHistoryRoleAccessException;

@Service
@Qualifier(AuthorisedGetCaseHistoryViewOperation.QUALIFIER)
public class AuthorisedGetCaseHistoryViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseHistoryViewOperation {

    public static final String QUALIFIER = "authorised-case-history";
    private final GetCaseHistoryViewOperation getCaseHistoryViewOperation;
    private final CaseAccessService caseAccessService;

    public AuthorisedGetCaseHistoryViewOperation(
        @Qualifier(DefaultGetCaseHistoryViewOperation.QUALIFIER)
            GetCaseHistoryViewOperation getCaseHistoryViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
        AccessControlService accessControlService,
        @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository,
        final CaseDataAccessControl caseDataAccessControl,
        CaseAccessService caseAccessService) {

        super(caseDefinitionRepository, accessControlService, caseDetailsRepository, caseDataAccessControl);
        this.getCaseHistoryViewOperation = getCaseHistoryViewOperation;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public CaseHistoryView execute(String caseReference, Long eventId) {
        CaseDetails caseDetails = getCase(caseReference);
        CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getCaseTypeId());
        Set<AccessProfile> accessProfiles = getAccessProfiles(caseDetails.getReferenceAsString());
        verifyCaseTypeReadAccess(caseTypeDefinition, accessProfiles);

        CaseHistoryView caseHistoryView = getCaseHistoryViewOperation.execute(caseReference, eventId);
        filterCaseTabFieldsByReadAccess(caseHistoryView, accessProfiles);
        filterAllowedTabsWithFields(caseHistoryView, accessProfiles);

        if (BooleanUtils.isTrue(caseAccessService.isExternalUser())) {
            throw new CaseHistoryRoleAccessException("Case History not accessible to the user");
        }
        return caseHistoryView;
    }
}
