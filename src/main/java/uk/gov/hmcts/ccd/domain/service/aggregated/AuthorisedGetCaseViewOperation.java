package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@Service
@Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER)
public class AuthorisedGetCaseViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseViewOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorisedGetCaseViewOperation.class);

    public static final String QUALIFIER = "authorised";
    private final GetCaseViewOperation getCaseViewOperation;

    public AuthorisedGetCaseViewOperation(
        final @Qualifier(DefaultGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final AccessControlService accessControlService,
        final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
        final @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
        final @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository) {
        super(caseDefinitionRepository, accessControlService, userRepository,
              caseUserRepository, caseDetailsRepository);
        this.getCaseViewOperation = getCaseViewOperation;
    }

    @Override
    public CaseView execute(String caseReference) {
        CaseView caseView = getCaseViewOperation.execute(caseReference);

        CaseTypeDefinition caseTypeDefinition = getCaseType(caseView.getCaseType().getId());
        String caseId = getCaseId(caseReference);
        Set<String> userRoles = getUserRoles(caseId);
        verifyCaseTypeReadAccess(caseTypeDefinition, userRoles);
        filterCaseTabFieldsByReadAccess(caseView, userRoles);
        filterAllowedTabsWithFields(caseView, userRoles);
        return filterUpsertAccess(caseReference, caseTypeDefinition, userRoles, caseView);
    }

    private void filterCaseTabFieldsByReadAccess(CaseView caseView, Set<String> userRoles) {
        caseView.setTabs(Arrays.stream(caseView.getTabs()).map(
            caseViewTab -> {
                caseViewTab.setFields(Arrays.stream(caseViewTab.getFields())
                    .filter(caseViewField -> getAccessControlService()
                        .canAccessCaseViewFieldWithCriteria(caseViewField, userRoles, CAN_READ))
                    .toArray(CaseViewField[]::new));
                return caseViewTab;
            }).toArray(CaseViewTab[]::new));
    }

    private CaseView filterUpsertAccess(String caseReference,
                                        CaseTypeDefinition caseTypeDefinition,
                                        Set<String> userRoles,
                                        CaseView caseView) {
        CaseViewActionableEvent[] authorisedActionableEvents;
        if (!getAccessControlService().canAccessCaseTypeWithCriteria(caseTypeDefinition,
                                                                     userRoles,
                                                                     CAN_UPDATE)
            || !getAccessControlService().canAccessCaseStateWithCriteria(caseView.getState().getId(),
                                                                         caseTypeDefinition,
                                                                         userRoles,
                                                                         CAN_UPDATE)) {
            authorisedActionableEvents = new CaseViewActionableEvent[]{};
            LOG.info("No authorised triggers for caseReference={} caseType={} version={} caseState={},"
                    + "caseTypeACLs={}, caseStateACLs={} userRoles={}",
                     caseReference,
                     caseTypeDefinition.getId(),
                     caseTypeDefinition.getVersion() != null ? caseTypeDefinition.getVersion().getNumber() : "",
                     caseView.getState() != null ? caseView.getState().getId() : "",
                     caseTypeDefinition.getAccessControlLists(),
                     caseTypeDefinition.getStates()
                         .stream()
                         .filter(cState -> cState.getId().equalsIgnoreCase(caseView.getState().getId()))
                         .map(CaseStateDefinition::getAccessControlLists)
                         .flatMap(Collection::stream)
                         .collect(toList()),
                     userRoles);
        } else {
            authorisedActionableEvents = getAccessControlService().filterCaseViewTriggersByCreateAccess(
                                                                                        caseView.getActionableEvents(),
                                                                                        caseTypeDefinition.getEvents(),
                                                                                        userRoles);
            LOG.debug("Authorised triggers for caseReference={} caseType={} version={} triggers={}",
                     caseReference,
                     caseTypeDefinition.getId(),
                     caseTypeDefinition.getVersion() != null ? caseTypeDefinition.getVersion().getNumber() : "",
                     Arrays.stream(authorisedActionableEvents).map(CaseViewActionableEvent::getName).collect(toList()));
        }

        caseView.setActionableEvents(authorisedActionableEvents);

        return caseView;
    }
}
