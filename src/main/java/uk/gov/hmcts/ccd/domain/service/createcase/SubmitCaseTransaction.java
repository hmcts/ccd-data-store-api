package uk.gov.hmcts.ccd.domain.service.createcase;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.DocumentMetadata;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ReferenceKeyUniqueConstraintException;
import uk.gov.hmcts.ccd.endpoint.std.CaseAccessEndpoint;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;

@Service
class SubmitCaseTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(SubmitCaseTransaction.class);

    @Inject
    private HttpServletRequest request;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final CaseTypeService caseTypeService;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;
    private final SecurityClassificationService securityClassificationService;
    private final CaseUserRepository caseUserRepository;
    private final UserAuthorisation userAuthorisation;
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;

    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT = "Document";
    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE = "document_binary_url";
    public static final String BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID = "DocumentId is not valid";
    public static final String HASH_CODE_STRING = "hashcode";
    public static final String CONTENT_TYPE = "content-type";



    @Inject
    public SubmitCaseTransaction(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                 final CaseAuditEventRepository caseAuditEventRepository,
                                 final CaseTypeService caseTypeService,
                                 final CallbackInvoker callbackInvoker,
                                 final UIDService uidService,
                                 final SecurityClassificationService securityClassificationService,
                                 final @Qualifier(CachedCaseUserRepository.QUALIFIER)  CaseUserRepository caseUserRepository,
                                 final UserAuthorisation userAuthorisation,
                                 @Qualifier("restTemplate") final RestTemplate restTemplate,
                                 ApplicationParams applicationParams,
                                 SecurityUtils securityUtils
                                 ) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.caseTypeService = caseTypeService;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.securityClassificationService = securityClassificationService;
        this.caseUserRepository = caseUserRepository;
        this.userAuthorisation = userAuthorisation;
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }

    @Transactional(REQUIRES_NEW)
    @Retryable(
        value = {ReferenceKeyUniqueConstraintException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 50)
    )
    public CaseDetails submitCase(Event event,
                                  CaseType caseType,
                                  IdamUser idamUser,
                                  CaseEvent eventTrigger,
                                  CaseDetails newCaseDetails, Boolean ignoreWarning) {

        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Set<String> documentSet = null;
        DocumentMetadata documentMetadata = null;

        newCaseDetails.setCreatedDate(now);
        newCaseDetails.setLastStateModifiedDate(now);
        newCaseDetails.setReference(Long.valueOf(uidService.generateUID()));

        boolean isApiVersion21 = request.getHeader(CONTENT_TYPE) != null
            && request.getHeader(CONTENT_TYPE).equals(V2.MediaType.CREATE_CASE_2_1);

        if (isApiVersion21) {
            LOG.debug("Creating case using Version 2.1 of case create API");
            documentSet = new HashSet<>();
            documentMetadata = DocumentMetadata.builder()
                                               .caseId(newCaseDetails.getReferenceAsString())
                                               .jurisdictionId(newCaseDetails.getJurisdiction())
                                               .caseTypeId(newCaseDetails.getCaseTypeId())
                                               .documents(new ArrayList<>())
                                               .build();

            extractDocumentFields(documentMetadata, newCaseDetails.getData(), documentSet);
        }

        /*
            About to submit

            TODO: Ideally, the callback should be outside of the transaction. However, it requires the case UID to have
            been assigned and the UID generation has to be part of a retryable transaction in order to recover from collisions.
         */
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse =
            callbackInvoker.invokeAboutToSubmitCallback(eventTrigger, null, newCaseDetails, caseType, ignoreWarning);
        // aboutToSubmitCallbackResponse -> filtering again, update the list as per response.
        // Match the documentId again, and produce URL list.
        // consider only removal scenario-> drop elements which are not found in original list.
        // Replacement scenario also -> drop elements which are not found in original list.
        // If document comes with hashcode, add it to POJO, else leave it

        //saveAuditEventForCaseDetails is making a call to caseDetailsRepository.set(newCaseDetails);
        //This is actually creating a record of the case in DB.
        final CaseDetails savedCaseDetails =
            saveAuditEventForCaseDetails(aboutToSubmitCallbackResponse, event, caseType, idamUser, eventTrigger, newCaseDetails);

        if (AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            caseUserRepository.grantAccess(Long.valueOf(savedCaseDetails.getId()),
                                           idamUser.getId(),
                                           CREATOR.getRole());
        }
        //Make a call to update the metadata into document store here.
        //the whole method is transactional, so it should be safe to update the metadata.
        //We should be catching any exception from document store, like timeout , duplicatekey etc and throw the exception again.
        //It is to be noted that the @Retryable will work only for ReferenceKeyUniqueConstraintException,
        // which is an exception while persisting case data

        if (isApiVersion21) {
            extractDocumentFields(documentMetadata, newCaseDetails.getData(), documentSet);
            filterDocumentFields(documentMetadata, documentSet);

            HttpEntity<DocumentMetadata> requestEntity = new HttpEntity<>(documentMetadata, securityUtils.authorizationHeaders());
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            ResponseEntity<Void> result = restTemplate.exchange(applicationParams.getCaseDocumentAmAPiHost().concat("/cases/documents/attachToCase"),
                                                                HttpMethod.PATCH, requestEntity, Void.class);
        }
        return savedCaseDetails;
    }

    private void extractDocumentFields(DocumentMetadata documentMetadata, Map<String, JsonNode> data, Set<String> documentSet) {
        data.forEach((field, jsonNodeValue) -> {
            //Check if the field consists of Document at any level, e.g. Complex fields can also have documents.
            //This quick check will reduce the processing time as most of filtering will be done at top level.
            if (jsonNodeValue != null && jsonNodeValue.get(HASH_CODE_STRING) != null) {
                //Check if current node is of type document and hashcode is available.
                JsonNode documentField = jsonNodeValue.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE);
                if (documentField != null && jsonNodeValue.get(HASH_CODE_STRING) != null
                    && !documentSet.contains(documentField.asText())) {

                    documentMetadata.getDocuments().add(CaseDocument
                                                            .builder()
                                                            .id(documentField.asText().substring(documentField.asText().length() - 36))
                                                            .hashToken(jsonNodeValue.get(HASH_CODE_STRING).asText())
                                                            .build());
                    if (jsonNodeValue instanceof ObjectNode) {
                        ((ObjectNode) jsonNodeValue).remove(HASH_CODE_STRING);
                    }
                    documentSet.add(documentField.asText().substring(documentField.asText().length() - 36));
                } else {
                    jsonNodeValue.fields().forEachRemaining(node -> extractDocumentFields(documentMetadata, (Map<String, JsonNode>) node, documentSet));
                }
            }
        });
    }

    private void filterDocumentFields(DocumentMetadata documentMetadata , Set<String> documentSet) {
        List<CaseDocument> caseDocumentList = documentMetadata.getDocuments().stream()
                                                              .filter(document -> documentSet.contains(document.getId()))
                                                              .collect(Collectors.toList());
        documentMetadata.setDocuments(caseDocumentList);

    }

    private CaseDetails saveAuditEventForCaseDetails(AboutToSubmitCallbackResponse response,
                                                     Event event,
                                                     CaseType caseType,
                                                     IdamUser idamUser,
                                                     CaseEvent eventTrigger,
                                                     CaseDetails newCaseDetails) {

        final CaseDetails savedCaseDetails = caseDetailsRepository.set(newCaseDetails);
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventId(event.getEventId());
        auditEvent.setEventName(eventTrigger.getName());
        auditEvent.setSummary(event.getSummary());
        auditEvent.setDescription(event.getDescription());
        auditEvent.setCaseDataId(savedCaseDetails.getId());
        auditEvent.setData(savedCaseDetails.getData());
        auditEvent.setStateId(savedCaseDetails.getState());
        CaseState caseState = caseTypeService.findState(caseType, savedCaseDetails.getState());
        auditEvent.setStateName(caseState.getName());
        auditEvent.setCaseTypeId(caseType.getId());
        auditEvent.setCaseTypeVersion(caseType.getVersion().getNumber());
        auditEvent.setUserId(idamUser.getId());
        auditEvent.setUserLastName(idamUser.getSurname());
        auditEvent.setUserFirstName(idamUser.getForename());
        auditEvent.setCreatedDate(newCaseDetails.getCreatedDate());
        auditEvent.setSecurityClassification(securityClassificationService.getClassificationForEvent(caseType, eventTrigger));
        auditEvent.setDataClassification(savedCaseDetails.getDataClassification());
        auditEvent.setSignificantItem(response.getSignificantItem());

        caseAuditEventRepository.set(auditEvent);
        return savedCaseDetails;
    }
}
