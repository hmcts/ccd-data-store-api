package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata.ACCESS_GRANTED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata.ACCESS_GRANTED_LABEL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@Service
@Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER)
public class AuthorisedGetCaseViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseViewOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorisedGetCaseViewOperation.class);

    public static final String QUALIFIER = "authorised";

    private final GetCaseViewOperation getCaseViewOperation;
    private final CaseDataAccessControl caseDataAccessControl;

    public AuthorisedGetCaseViewOperation(
        final @Qualifier(DefaultGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final AccessControlService accessControlService,
        final @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository,
        CaseDataAccessControl caseDataAccessControl) {
        super(caseDefinitionRepository, accessControlService, caseDetailsRepository, caseDataAccessControl);
        this.getCaseViewOperation = getCaseViewOperation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public CaseView execute(String caseReference) {
        CaseView caseView = getCaseViewOperation.execute(caseReference);

        CaseTypeDefinition caseTypeDefinition = getCaseType(caseView.getCaseType().getId());
        Set<String> accessProfiles = getAccessProfiles(caseReference);
        verifyCaseTypeReadAccess(caseTypeDefinition, accessProfiles);
        filterCaseTabFieldsByReadAccess(caseView, accessProfiles);
        filterAllowedTabsWithFields(caseView, accessProfiles);
        updateWithAccessControlMetadata(caseView);
        return filterUpsertAccess(caseReference, caseTypeDefinition, accessProfiles, caseView);
    }

    private void updateWithAccessControlMetadata(CaseView caseView) {
        CaseAccessMetadata caseAccessMetadata = caseDataAccessControl.generateAccessMetadata(caseView.getCaseId());

        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(ACCESS_GRANTED);
        caseViewField.setLabel(ACCESS_GRANTED_LABEL);
        caseViewField.setSecurityLabel(SecurityClassification.PUBLIC.name());
        caseViewField.setValue(caseAccessMetadata.getAccessGrantsString());

        caseView.addMetadataFields(List.of(caseViewField));
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
