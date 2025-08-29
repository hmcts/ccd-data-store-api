package uk.gov.hmcts.ccd.domain.service.aggregated;

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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata.ACCESS_GRANTED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata.ACCESS_GRANTED_LABEL;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata.ACCESS_PROCESS;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata.ACCESS_PROCESS_LABEL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@Service
@Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER)
public class AuthorisedGetCaseViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseViewOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorisedGetCaseViewOperation.class);

    public static final String QUALIFIER = "authorised";
    public static final String TEXT = "Text";

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
        Set<AccessProfile> accessProfiles = getAccessProfiles(caseReference);
        verifyCaseTypeReadAccess(caseTypeDefinition, accessProfiles);
        filterCaseTabFieldsByReadAccess(caseView, accessProfiles);
        filterAllowedTabsWithFields(caseView, accessProfiles);
        updateWithAccessControlMetadata(caseView);
        return filterUpsertAccess(caseReference, caseTypeDefinition, accessProfiles, caseView);
    }

    private void updateWithAccessControlMetadata(CaseView caseView) {
        CaseAccessMetadata caseAccessMetadata = caseDataAccessControl.generateAccessMetadata(caseView.getCaseId());

        List<CaseViewField> metadataFieldsToAdd = new ArrayList<>();

        if (caseAccessMetadata.getAccessGrants() != null) {
            metadataFieldsToAdd.add(createCaseViewField(ACCESS_GRANTED,
                ACCESS_GRANTED_LABEL,
                caseAccessMetadata.getAccessGrantsString()));
        }

        if (caseAccessMetadata.getAccessProcess() != null) {
            metadataFieldsToAdd.add(createCaseViewField(ACCESS_PROCESS,
                ACCESS_PROCESS_LABEL,
                caseAccessMetadata.getAccessProcessString()));
        }

        if (!metadataFieldsToAdd.isEmpty()) {
            caseView.addMetadataFields(metadataFieldsToAdd);
        }
    }

    private CaseViewField createCaseViewField(
                                     String accessGranted,
                                     String accessGrantedLabel,
                                     String caseViewFieldValue) {
        var fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(TEXT);
        fieldTypeDefinition.setType(TEXT);

        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(accessGranted);
        caseViewField.setLabel(accessGrantedLabel);
        caseViewField.setSecurityLabel(SecurityClassification.PUBLIC.name());
        caseViewField.setValue(caseViewFieldValue);
        caseViewField.setFieldTypeDefinition(fieldTypeDefinition);
        caseViewField.setMetadata(true);
        return caseViewField;
    }

    private CaseView filterUpsertAccess(String caseReference,
                                        CaseTypeDefinition caseTypeDefinition,
                                        Set<AccessProfile> userRoles,
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
