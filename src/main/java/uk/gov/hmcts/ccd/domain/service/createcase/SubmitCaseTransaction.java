package uk.gov.hmcts.ccd.domain.service.createcase;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataParsingException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ReferenceKeyUniqueConstraintException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

@Service
class SubmitCaseTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(SubmitCaseTransaction.class);

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
    public static final String BINARY = "/binary";
    public static final String CASE_DATA_PARSING_EXCEPTION = "Exception while extracting the document fields from Case payload";
    public static final String DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION = "The documents have been altered outside the create case transaction";

    @Inject
    public SubmitCaseTransaction(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                 final CaseAuditEventRepository caseAuditEventRepository,
                                 final CaseTypeService caseTypeService,
                                 final CallbackInvoker callbackInvoker,
                                 final UIDService uidService,
                                 final SecurityClassificationService securityClassificationService,
                                 final @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
                                 final UserAuthorisation userAuthorisation,
                                 @Qualifier("restTemplate") final RestTemplate restTemplate,
                                 ApplicationParams applicationParams,
                                 SecurityUtils securityUtils,
                                 HttpServletRequest request
                                ) {
        this.request = request;
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
        Set<String> documentSetBeforeCallback = null;
        Set<String> documentAfterCallback = null;
        DocumentMetadata documentMetadata = null;

        newCaseDetails.setCreatedDate(now);
        newCaseDetails.setLastStateModifiedDate(now);
        newCaseDetails.setReference(Long.valueOf(uidService.generateUID()));

        boolean isApiVersion21 = request.getContentType() != null
            && request.getContentType().equals(V2.MediaType.CREATE_CASE_2_1);

        if (isApiVersion21) {
            try {
                LOG.debug("Creating case using Version 2.1 of case create API");
                documentSetBeforeCallback = new HashSet<>();
                documentMetadata = DocumentMetadata.builder()
                                                   .caseId(newCaseDetails.getReferenceAsString())
                                                   .jurisdictionId(newCaseDetails.getJurisdiction())
                                                   .caseTypeId(newCaseDetails.getCaseTypeId())
                                                   .documents(new ArrayList<>())
                                                   .build();

                extractDocumentFields(documentMetadata, newCaseDetails.getData(), documentSetBeforeCallback);
            }
            catch (Exception e) {
                LOG.error(CASE_DATA_PARSING_EXCEPTION);
                throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
            }
        }

        /*
            About to submit

            TODO: Ideally, the callback should be outside of the transaction. However, it requires the case UID to have
            been assigned and the UID generation has to be part of a retryable transaction in order to recover from collisions.
         */
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse =
            callbackInvoker.invokeAboutToSubmitCallback(eventTrigger, null, newCaseDetails, caseType, ignoreWarning);

        //saveAuditEventForCaseDetails is making a call to caseDetailsRepository.set(newCaseDetails);
        //This is actually creating a record of the case in DB.
        final CaseDetails savedCaseDetails =
            saveAuditEventForCaseDetails(aboutToSubmitCallbackResponse, event, caseType, idamUser, eventTrigger, newCaseDetails);

        if (AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            caseUserRepository.grantAccess(Long.valueOf(savedCaseDetails.getId()),
                                           idamUser.getId(),
                                           CREATOR.getRole());
        }

        if (isApiVersion21) {
            attachDocumentToCase(newCaseDetails, documentSetBeforeCallback, documentMetadata);
        }
        return savedCaseDetails;
    }

    private void attachDocumentToCase(CaseDetails newCaseDetails, Set<String> documentSetBeforeCallback, DocumentMetadata documentMetadata) {
        Set<String> documentAfterCallback;
        documentAfterCallback = new HashSet<>();
        extractDocumentFields(documentMetadata, newCaseDetails.getData(), documentAfterCallback);
        filterDocumentFields(documentMetadata, documentSetBeforeCallback, documentAfterCallback);

        try {
            if (!documentMetadata.getDocuments().isEmpty()) {
                HttpEntity<DocumentMetadata> requestEntity = new HttpEntity<>(documentMetadata, securityUtils.authorizationHeaders());
                restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

                ResponseEntity<Boolean> result = restTemplate
                    .exchange(applicationParams.getCaseDocumentAmApiHost().concat(applicationParams.getAttachDocumentPath()),
                              HttpMethod.PATCH, requestEntity, Boolean.class);

                if (!result.getStatusCode().equals(HttpStatus.OK) || result.getBody() == null || result.getBody().equals(false)) {
                    LOG.error(DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION);
                    throw new CaseConcurrencyException(DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION);
                }
            }
        } catch (Exception e) {
            LOG.error(DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION);
            throw new CaseConcurrencyException(DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION);
        }
    }

     void extractDocumentFields(DocumentMetadata documentMetadata, Map<String, JsonNode> data, Set<String> documentSet) {
        try {
            data.forEach((field, jsonNode) -> {
                //Check if the field consists of Document at any level, e.g. Complex fields can also have documents.
                //This quick check will reduce the processing time as most of filtering will be done at top level.
                if (jsonNode != null && jsonNode.findValue(HASH_CODE_STRING) != null) {

                    //Document Binary URL is preferred.
                    JsonNode documentField = jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null ?
                                             jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) :
                                             jsonNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE);
                    //Check if current node is of type document and hashcode is available.

                    if (documentField != null && jsonNode.get(HASH_CODE_STRING) != null && !documentSet.contains(documentField.asText())) {
                        String documentId = extractDocumentId(documentField);

                        documentMetadata.getDocuments().add(CaseDocument
                                                                .builder()
                                                                .id(documentId)
                                                                .hashToken(jsonNode.get(HASH_CODE_STRING).asText())
                                                                .permissions(Collections.singletonList(Permission.CREATE))
                                                                .build());
                        if (jsonNode instanceof ObjectNode) {
                            ((ObjectNode) jsonNode).remove(HASH_CODE_STRING);
                        }
                        documentSet.add(documentId);
                    }
                    else {
                        jsonNode.fields().forEachRemaining(node -> extractDocumentFields(documentMetadata,
                                                                                         Collections.singletonMap(node.getKey(), node.getValue()), documentSet));
                    }
                }
            });
        } catch (Exception e) {
            LOG.error(CASE_DATA_PARSING_EXCEPTION);
            throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
        }
    }

    private String extractDocumentId(JsonNode documentField) {
        if (documentField.asText().contains(BINARY)) {
            return documentField.asText().substring(documentField.asText().length() - 43, documentField.asText().length() - 7);
        } else {
            return documentField.asText().substring(documentField.asText().length() - 36);
        }
    }

    private void filterDocumentFields(DocumentMetadata documentMetadata, Set<String> documentSetBeforeCallback, Set<String> documentSetAfterCallback) {
        try {
            //The below change is STRICTLY for LOCAL testing purpose. It needs to be removed in the PR environment
            documentSetAfterCallback.addAll(documentSetBeforeCallback);
            //PLEASE REMOVE ABOVE line in PR environment


            //find documents which are intersection of Before and after callback
            Set<String> filteredDocumentSet = documentSetAfterCallback.stream()
                                                                      .filter(documentSetBeforeCallback::contains)
                                                                      .collect(Collectors.toSet());

            //Add the intersection to aftercallback list. Now, afterCallbackList will have the documents from
            //Callback response
            // + original documents which have not been removed by the callback
            // + Any new documents which are added by callback response
            //This code should drop any documents which were removed by the callback
            documentSetAfterCallback.addAll(filteredDocumentSet);

            //The following code will filter the documents based on above prepared Set.
            List<CaseDocument> caseDocumentList = documentMetadata.getDocuments()
                                                                  .stream()
                                                                  .filter(document -> documentSetAfterCallback.contains(document.getId()))
                                                                  .collect(Collectors.toList());
            documentMetadata.setDocuments(caseDocumentList);
        } catch (Exception e) {
            LOG.error("Exception while filtering the document fields.");
            throw new DataParsingException("Exception while filtering the document fields.");
        }
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
