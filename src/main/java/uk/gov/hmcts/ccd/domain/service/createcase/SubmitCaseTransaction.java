package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ReferenceKeyUniqueConstraintException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;
import uk.gov.hmcts.ccd.ApplicationParams;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class SubmitCaseTransaction implements AccessControl {

    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final CaseTypeService caseTypeService;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;
    private final SecurityClassificationService securityClassificationService;
    private final CaseDataAccessControl caseDataAccessControl;
    private final MessageService messageService;
    private final CaseDocumentService caseDocumentService;
    private final ApplicationParams applicationParams;
    private final CaseAccessGroupUtils caseAccessGroupUtils;
    private final CaseDocumentTimestampService caseDocumentTimestampService;

    private static final String ORGANISATION_POLICY_FIELD = "OrganisationPolicyField";
    private static final String ORGANISATION = "Organisation";
    private static final String ORGANISATIONID = "OrganisationID";
    private static final String ORG_POLICY_NEW_CASE = "newCase";
    private static final String SUPPLEMENTRY_DATA_NEW_CASE = "new_case";
    private static final Logger LOG = LoggerFactory.getLogger(SubmitCaseTransaction.class);

    @Inject
    public SubmitCaseTransaction(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                     final CaseDetailsRepository caseDetailsRepository,
                                    final CaseAuditEventRepository caseAuditEventRepository,
                                    final CaseTypeService caseTypeService,
                                    final CallbackInvoker callbackInvoker,
                                    final UIDService uidService,
                                    final SecurityClassificationService securityClassificationService,
                                    final CaseDataAccessControl caseDataAccessControl,
                                    final @Qualifier("caseEventMessageService") MessageService messageService,
                                    final CaseDocumentService caseDocumentService,
                                    final ApplicationParams applicationParams,
                                    final CaseAccessGroupUtils caseAccessGroupUtils,
                                    final CaseDocumentTimestampService caseDocumentTimestampService
                                 ) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.caseTypeService = caseTypeService;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.securityClassificationService = securityClassificationService;
        this.caseDataAccessControl = caseDataAccessControl;
        this.messageService = messageService;
        this.caseDocumentService = caseDocumentService;
        this.applicationParams = applicationParams;
        this.caseAccessGroupUtils = caseAccessGroupUtils;
        this.caseDocumentTimestampService = caseDocumentTimestampService;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        value = {ReferenceKeyUniqueConstraintException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 50)
    )

    public CaseDetails submitCase(Event event,
                                  CaseTypeDefinition caseTypeDefinition,
                                  IdamUser idamUser,
                                  CaseEventDefinition caseEventDefinition,
                                  CaseDetails caseDetails,
                                  Boolean ignoreWarning,
                                  IdamUser onBehalfOfUser) {

        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        caseDetails.setCreatedDate(now);
        caseDetails.setLastStateModifiedDate(now);
        caseDetails.setReference(Long.valueOf(uidService.generateUID()));

        final CaseDetails caseDetailsWithoutHashes = caseDocumentService.stripDocumentHashes(caseDetails);

        /*
            About to submit

            TODO: Ideally, the callback should be outside of the transaction. However, it requires the case UID to have
            been assigned and the UID generation has to be part of a retryable transaction in order to recover from
            collisions.
         */
        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(
            caseEventDefinition,
            null,
            caseDetailsWithoutHashes,
            caseTypeDefinition,
            ignoreWarning
        );

        caseDocumentTimestampService.addUploadTimestamps(caseDetailsWithoutHashes, null);

        @SuppressWarnings("UnnecessaryLocalVariable")
        final CaseDetails caseDetailsAfterCallback = caseDetailsWithoutHashes;

        final List<DocumentHashToken> documentHashes = caseDocumentService.extractDocumentHashToken(
            caseDetails.getData(),
            caseDetailsAfterCallback.getData()
        );

        final CaseDetails caseDetailsAfterCallbackWithoutHashes = caseDocumentService.stripDocumentHashes(
            caseDetailsAfterCallback
        );

        if (this.applicationParams.getCaseGroupAccessFilteringEnabled()) {
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetailsAfterCallbackWithoutHashes,
                caseTypeDefinition);
        }

        // Identify organizations with newCase set to true
        List<JsonNode> organizations = getOrganizationsWithNewCaseTrue(caseDetailsAfterCallbackWithoutHashes);

        // Update case supplementary data
        updateCaseSupplementaryData(caseDetailsAfterCallbackWithoutHashes, organizations);

        // Clear newCase attributes
        clearNewCaseAttributes(caseDetailsAfterCallbackWithoutHashes);

        final CaseDetails savedCaseDetails = saveAuditEventForCaseDetails(
            aboutToSubmitCallbackResponse,
            event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            caseDetailsAfterCallbackWithoutHashes,
            onBehalfOfUser
        );

        caseDataAccessControl.grantAccess(savedCaseDetails, idamUser.getId());

        caseDocumentService.attachCaseDocuments(
            caseDetails.getReferenceAsString(),
            caseDetails.getCaseTypeId(),
            caseDetails.getJurisdiction(),
            documentHashes
        );

        return savedCaseDetails;
    }

    private CaseDetails saveAuditEventForCaseDetails(AboutToSubmitCallbackResponse response,
                                                     Event event,
                                                     CaseTypeDefinition caseTypeDefinition,
                                                     IdamUser idamUser,
                                                     CaseEventDefinition caseEventDefinition,
                                                     CaseDetails newCaseDetails,
                                                     IdamUser onBehalfOfUser) {

        final CaseDetails savedCaseDetails = caseDetailsRepository.set(newCaseDetails);
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventId(event.getEventId());
        auditEvent.setEventName(caseEventDefinition.getName());
        auditEvent.setSummary(event.getSummary());
        auditEvent.setDescription(event.getDescription());
        auditEvent.setCaseDataId(savedCaseDetails.getId());
        auditEvent.setData(savedCaseDetails.getData());
        auditEvent.setStateId(savedCaseDetails.getState());
        CaseStateDefinition caseStateDefinition =
            caseTypeService.findState(caseTypeDefinition, savedCaseDetails.getState());
        auditEvent.setStateName(caseStateDefinition.getName());
        auditEvent.setCaseTypeId(caseTypeDefinition.getId());
        auditEvent.setCaseTypeVersion(caseTypeDefinition.getVersion().getNumber());
        auditEvent.setCreatedDate(newCaseDetails.getCreatedDate());
        auditEvent.setSecurityClassification(securityClassificationService.getClassificationForEvent(caseTypeDefinition,
            caseEventDefinition));
        auditEvent.setDataClassification(savedCaseDetails.getDataClassification());
        auditEvent.setSignificantItem(response.getSignificantItem());
        saveUserDetails(idamUser, onBehalfOfUser, auditEvent);

        caseAuditEventRepository.set(auditEvent);

        messageService.handleMessage(MessageContext.builder()
            .caseDetails(savedCaseDetails)
            .caseTypeDefinition(caseTypeDefinition)
            .caseEventDefinition(caseEventDefinition)
            .oldState(null).build());
        return savedCaseDetails;
    }

    private void saveUserDetails(IdamUser idamUser, IdamUser onBehalfOfUser, AuditEvent auditEvent) {
        if (onBehalfOfUser == null) {
            auditEvent.setUserId(idamUser.getId());
            auditEvent.setUserLastName(idamUser.getSurname());
            auditEvent.setUserFirstName(idamUser.getForename());
        } else {
            auditEvent.setUserId(onBehalfOfUser.getId());
            auditEvent.setUserLastName(onBehalfOfUser.getSurname());
            auditEvent.setUserFirstName(onBehalfOfUser.getForename());
            auditEvent.setProxiedBy(idamUser.getId());
            auditEvent.setProxiedByLastName(idamUser.getSurname());
            auditEvent.setProxiedByFirstName(idamUser.getForename());
        }
    }

    public List<JsonNode> getOrganizationsWithNewCaseTrue(CaseDetails caseDetails) {
        List<JsonNode> newCaseOrganizations = new ArrayList<>();
        JsonNode orgPolicyJsonNodes = caseDetails.getData().get(ORGANISATION_POLICY_FIELD);

        if (orgPolicyJsonNodes != null) {
            if (orgPolicyJsonNodes.has(ORG_POLICY_NEW_CASE)
                && orgPolicyJsonNodes.get(ORG_POLICY_NEW_CASE).asBoolean()) {
                if (orgPolicyJsonNodes.has(ORGANISATION)) {
                    newCaseOrganizations.add(orgPolicyJsonNodes.get(ORGANISATION));
                }
            }
        }
        LOG.debug("Organisation found for caseType={} version={} newCaseOrganizations={}.",
            caseDetails.getCaseTypeId(), caseDetails.getVersion(), newCaseOrganizations);
        return newCaseOrganizations;
    }

    public void updateCaseSupplementaryData(CaseDetails caseDetails, List<JsonNode> organizations) {
        Map<String, JsonNode> supplementaryData = caseDetails.getSupplementaryData();
        if (supplementaryData == null) {
            supplementaryData = new HashMap<>();
        }

        for (JsonNode organization : organizations) {
            JsonNode orgNode = new ObjectMapper().createObjectNode()
                .put(organization.get(ORGANISATIONID).textValue(), Boolean.TRUE.toString());
            supplementaryData.put(SUPPLEMENTRY_DATA_NEW_CASE, orgNode);
        }

        LOG.debug("SupplementaryData ={} .", supplementaryData);
        caseDetails.setSupplementaryData(supplementaryData);
    }

    public void clearNewCaseAttributes(CaseDetails caseDetails) {
        JsonNode orgPolicyJsonNodes = caseDetails.getData().get(ORGANISATION_POLICY_FIELD);

        if (orgPolicyJsonNodes != null) {
            ((ObjectNode) orgPolicyJsonNodes).remove(ORG_POLICY_NEW_CASE);
        }
    }
}
