package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataParsingException;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

public class CaseDocumentAttachOperation {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAttachOperation.class);

    Map<String, String> documentTokenMap = null;
    Map<String, String> documentAfterCallback = null;
    Map<String, String> documentAfterCallbackOriginalCopy = new HashMap<>();
    CaseDocumentsMetadata caseDocumentsMetadata = null;
    public static final String CASE_DATA_PARSING_EXCEPTION = "Exception while extracting the document fields from Case payload";
    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT = "Document";
    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_BINARY_URL = "document_binary_url";
    public static final String HASH_TOKEN_STRING = "hashToken";

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    public static final String BINARY = "/binary";

    public CaseDocumentAttachOperation(RestTemplate restTemplate,
                                       ApplicationParams applicationParams,
                                       SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }

    public void beforeCallbackPrepareDocumentMetaData(Map<String, JsonNode> caseData) {
        documentTokenMap = new HashMap<>();
        extractDocumentFieldsBeforeCallback(caseData, documentTokenMap);
    }

    public void afterCallbackPrepareDocumentMetaData(CaseDetails caseDetails, boolean callbackWasCalled) {
        try {
            documentAfterCallback = new HashMap<>();
            caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                         .caseId(caseDetails.getReference().toString())
                                                         .caseTypeId(caseDetails.getCaseTypeId())
                                                         .jurisdictionId(caseDetails.getJurisdiction())
                                                         .documentHashToken(new ArrayList<>())
                                                         .build();

            if (callbackWasCalled)
            // to remove hashcode before compute delta
            {
                extractDocumentFieldsAfterCallback(caseDocumentsMetadata, caseDetails.getData(), documentAfterCallback);
            }
        } catch (Exception e) {
            LOG.error(CASE_DATA_PARSING_EXCEPTION);
            throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
        }
    }

    public void attachDocumentDuringCaseCreation(CaseDetails caseDetails, boolean isCallbackResponseValid) {
        afterCallbackPrepareDocumentMetaData(caseDetails, isCallbackResponseValid);
        filterDocumentFields();
    }
    public void filterDocumentFields() {
        filterDocumentFields(caseDocumentsMetadata, documentTokenMap, documentAfterCallback);
    }

    public void restCallToAttachCaseDocuments() {
        HttpEntity<CaseDocumentsMetadata> requestEntity = new HttpEntity<>(caseDocumentsMetadata, securityUtils.authorizationHeaders());
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        if (!caseDocumentsMetadata.getDocumentHashToken().isEmpty()) {
            try {
                restTemplate.exchange(applicationParams.getCaseDocumentAmApiHost().concat(applicationParams.getAttachDocumentPath()),
                                      HttpMethod.PATCH, requestEntity, Void.class);

            } catch (HttpClientErrorException restClientException) {
                //handle 400's
                if (restClientException.getStatusCode() != HttpStatus.FORBIDDEN) {
                    throw restClientException;
                }
                String badDocument = restClientException.getResponseBodyAsString();

                if (documentAfterCallbackOriginalCopy.size() > 0 && documentAfterCallbackOriginalCopy.get(badDocument) != null) {
                    throw new ServiceException(String.format("The document %s introduced by Services has invalid hashToken", badDocument));
                } else {
                    throw new DocumentTokenException(String.format("The user has provided an invalid hashToken for document %s", badDocument));
                }
            }
        }
    }

    public void extractDocumentFieldsBeforeCallback(Map<String, JsonNode> data, Map<String, String> documentTokenMap) {
        data.forEach((field, jsonNode) -> {
            if (jsonNode != null && isDocumentField(jsonNode)) {
                if (jsonNode.get(HASH_TOKEN_STRING) == null) {
                    throw new BadRequestException("Hash token is not provided for the document.");
                }

                String documentId = extractDocumentId(jsonNode);
                documentTokenMap.put(documentId, jsonNode.get(HASH_TOKEN_STRING).asText());
                if (jsonNode instanceof ObjectNode) {
                    ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
                }
            } else {
                jsonNode.fields().forEachRemaining(node -> extractDocumentFieldsBeforeCallback(
                    Collections.singletonMap(node.getKey(), node.getValue()), documentTokenMap));
            }
        });
    }

    public void extractDocumentFieldsAfterCallback(CaseDocumentsMetadata caseDocumentsMetadata, Map<String, JsonNode> data, Map<String, String> documentMap) {
        data.forEach((field, jsonNode) -> {
            //Check if the field consists of Document at any level, e.g. Complex fields can also have documents.
            //This quick check will reduce the processing time as most of filtering will be done at top level.
            //****** Every document should have hashcode, else throw error
            if (jsonNode != null && isDocumentField(jsonNode)) {
                if (jsonNode.get(HASH_TOKEN_STRING) == null) {
                    throw new BadRequestException("The document does not has the hashcode");
                }
                String documentId = extractDocumentId(jsonNode);
                documentMap.put(documentId, jsonNode.get(HASH_TOKEN_STRING).asText());
                caseDocumentsMetadata.getDocumentHashToken().add(DocumentHashToken.builder()
                                                                                  .id(documentId)
                                                                                  .hashToken(jsonNode.get(HASH_TOKEN_STRING).asText())
                                                                                  .build());

                if (jsonNode instanceof ObjectNode) {
                    ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
                }

            } else {
                jsonNode.fields().forEachRemaining(node -> extractDocumentFieldsAfterCallback(caseDocumentsMetadata,
                                                                                              Collections.singletonMap(node.getKey(), node.getValue()),
                                                                                              documentMap));
            }
        });
    }

    private boolean isDocumentField(JsonNode jsonNode) {
        return jsonNode.get(DOCUMENT_BINARY_URL) != null
               || jsonNode.get(DOCUMENT_URL) != null;
    }

    public String extractDocumentId(JsonNode jsonNode) {
        JsonNode documentField = jsonNode.get(DOCUMENT_BINARY_URL) != null ?
                                 jsonNode.get(DOCUMENT_BINARY_URL) :
                                 jsonNode.get(DOCUMENT_URL);

        if (documentField.asText().contains(BINARY)) {
            return documentField.asText().substring(documentField.asText().length() - 43, documentField.asText().length() - 7);
        } else {
            return documentField.asText().substring(documentField.asText().length() - 36);
        }
    }


    public void filterDocumentFields(CaseDocumentsMetadata caseDocumentsMetadata, Map<String, String> documentTokenMap,
                                     Map<String, String> documentAfterCallback) {
        if (documentAfterCallback.size() > 0) {
            // Keep a copy of the original documents after callback
            // It will be used to check if services has tempered with the hashtoken
            documentAfterCallbackOriginalCopy.putAll(documentAfterCallback);

            // find ids of document inside before call back map which are coming through after call back map
            List<String> commonDocumentIds = documentAfterCallback.keySet().stream().filter(documentTokenMap::containsKey)
                                                                     .collect(Collectors.toList());

            //find Hash token of documents belong to  before call back which are in After callback Map
            Map<String, String> commonDocumentIdWithHashtoken = documentTokenMap.entrySet()
                                                                                         .stream()
                                                                                         .filter(e -> commonDocumentIds.contains(e.getKey()))
                                                                                         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            //Compare the hashcode with services response. error 500
            //putting back documentIds with hashToken in after call back map  which belong to before callback Map
            documentAfterCallback.putAll(commonDocumentIdWithHashtoken);

            //  filter after callback  map having hash token
            Map<String, String> finalDocumentsWithHashtoken = documentAfterCallback.entrySet().stream().filter(e -> e.getValue() != null)
                                                                                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            //Filter DocumentHashToken based on finalDocumentsWithHashtoken
            //refactor
            finalDocumentsWithHashtoken.forEach((key, value) ->
                                                    caseDocumentsMetadata.getDocumentHashToken().add(DocumentHashToken
                                                                                                         .builder()
                                                                                                         .id(key)
                                                                                                         .hashToken(value)
                                                                                                         .build()));
        } else {
            documentTokenMap.forEach((key, value) ->
                                                  caseDocumentsMetadata.getDocumentHashToken().add(DocumentHashToken
                                                                                                       .builder()
                                                                                                       .id(key)
                                                                                                       .hashToken(value)
                                                                                                       .build())
                                             );

        }

    }

    public Set<String> differenceBeforeAndAfterInCaseDetails(final CaseDetails caseDetails, final Map<String, JsonNode> caseData) {

        final Map<String, JsonNode> documentsDifference = new HashMap<>();
        final Set<String> filterDocumentSet = new HashSet<>();

        if (null == caseData) {
            return filterDocumentSet;
        }

        caseData.forEach((key, value) -> {

            if (caseDetails.getData().containsKey(key) && (value.findValue(DOCUMENT_BINARY_URL) != null || value.findValue(
                DOCUMENT_URL) != null)) {
                if (!value.equals(caseDetails.getData().get(key))) {
                    documentsDifference.put(key, value);
                }
            } else if (value.findValue(DOCUMENT_BINARY_URL) != null || value.findValue(DOCUMENT_URL) != null) {
                documentsDifference.put(key, value);
            }
        });
        //Find documentId based on filter Map. So that I can filter the DocumentMetaData Object before calling the case document am Api.
        findDocumentsId(documentsDifference, filterDocumentSet);
        return filterDocumentSet;
    }

    private void findDocumentsId(Map<String, JsonNode> sanitisedDataToAttachDoc, Set<String> filterDocumentSet) {

        sanitisedDataToAttachDoc.forEach((field, jsonNode) -> {
            //Check if the field consists of Document at any level, e.g. Complex fields can also have documents.
            //This quick check will reduce the processing time as most of filtering will be done at top level.
            //****** Every document should have hashcode, else throw error
            if (jsonNode != null && isDocumentField(jsonNode)) {
                String documentId = extractDocumentId(jsonNode);
                filterDocumentSet.add(documentId);

            } else {
                jsonNode.fields().forEachRemaining(node -> findDocumentsId(
                    Collections.singletonMap(node.getKey(), node.getValue()), filterDocumentSet));
            }
        });
    }
}
