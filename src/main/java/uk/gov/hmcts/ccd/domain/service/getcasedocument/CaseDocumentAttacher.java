package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import liquibase.util.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;


public class CaseDocumentAttacher {

    Map<String, String> documentsBeforeCallback = new HashMap<>();
    Map<String, String> documentsAfterCallback = null;
    Map<String, String> documentAfterCallbackOriginalCopy = new HashMap<>();
    Map<String, JsonNode> recursiveMapForCaseDetailsBefore = new HashMap<>();
    CaseDocumentsMetadata caseDocumentsMetadata = null;
    Set<String> existingDocumentsInCase = new HashSet<>();

    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT = "Document";
    public static final String DOCUMENT_URL = "document_url";
    public static final String DOCUMENT_BINARY_URL = "document_binary_url";
    public static final String HASH_TOKEN_STRING = "hashToken";
    public static final String BINARY = "/binary";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String CMC_EVENT_UPDATE = "ReviewedPaperResponse";

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    JsonNode caseBeforeNode;

    public CaseDocumentAttacher(RestTemplate restTemplate,
                                ApplicationParams applicationParams,
                                SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }


    public void extractDocumentsWithHashTokenBeforeCallbackForCreateCase(Map<String, JsonNode> data) {
        data.forEach((field, jsonNode) -> {
            if (!jsonNode.isNull() && isDocumentField(jsonNode)) {
                String documentId = extractDocumentId(jsonNode);
                if (jsonNode.get(HASH_TOKEN_STRING) == null) {
                    throw new BadRequestException(String.format("The document %s does not has the hashToken", documentId));
                }
                documentsBeforeCallback.put(documentId, jsonNode.get(HASH_TOKEN_STRING).asText());
                ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
            } else {
                if (jsonNode instanceof ArrayNode) {
                    Iterator<JsonNode> arrayNode = ((ArrayNode) jsonNode).elements();
                    while (arrayNode.hasNext()) {
                        JsonNode arrayNodeElement = arrayNode.next();
                        arrayNodeElement.fields().forEachRemaining(node -> extractDocumentsWithHashTokenBeforeCallbackForCreateCase(
                            Collections.singletonMap(node.getKey(), node.getValue())));

                    }

                } else {
                    jsonNode.fields().forEachRemaining(node -> extractDocumentsWithHashTokenBeforeCallbackForCreateCase(
                        Collections.singletonMap(node.getKey(), node.getValue())));
                }
            }
        });
    }

    public void extractDocumentsWithHashTokenBeforeCallbackForUpdate(Map<String, JsonNode> data, CaseDetails caseDetailsBefore) {
        data.forEach((field, jsonNode) -> {
            if (!jsonNode.isNull() && isDocumentField(jsonNode)) {
                String documentId = extractDocumentId(jsonNode);
                if (jsonNode.get(HASH_TOKEN_STRING) != null) {
                    documentsBeforeCallback.put(documentId, jsonNode.get(HASH_TOKEN_STRING).asText());
                    ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
                } else if (caseDetailsBefore != null) {
                    existingDocument(caseDetailsBefore.getData(), documentId);
                    if (existingDocumentsInCase.isEmpty()) {
                        throw new BadRequestException(String.format("The document %s does not has the hashToken", documentId));
                    } else {
                        existingDocumentsInCase.clear();
                    }
                }
            } else {
                if (jsonNode instanceof ArrayNode) {
                    Iterator<JsonNode> arrayNode = ((ArrayNode) jsonNode).elements();
                    while (arrayNode.hasNext()) {
                        JsonNode arrayNodeElement = arrayNode.next();
                        arrayNodeElement.fields().forEachRemaining(node -> extractDocumentsWithHashTokenBeforeCallbackForUpdate(
                            Collections.singletonMap(node.getKey(), node.getValue()), caseDetailsBefore));

                    }
                } else {
                    jsonNode.fields().forEachRemaining(node -> extractDocumentsWithHashTokenBeforeCallbackForUpdate(
                        Collections.singletonMap(node.getKey(), node.getValue()), caseDetailsBefore));
                }
            }
        });
    }

    public void extractDocumentsAfterCallBack(CaseDetails caseDetails, boolean callBackWasCalled) {

        documentsAfterCallback = new HashMap<>();
        caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(caseDetails.getReference().toString())
            .caseTypeId(caseDetails.getCaseTypeId())
            .jurisdictionId(caseDetails.getJurisdiction())
            .documentHashToken(new ArrayList<>())
            .build();

        if (callBackWasCalled) {
            // to remove hashcode before compute delta
            extractDocumentIdsAfterCallback(caseDetails.getData(), documentsAfterCallback);
        }
    }

    private void extractDocumentIdsAfterCallback(Map<String, JsonNode> data, Map<String, String> documentMap) {
        data.forEach((field, jsonNode) -> {
            if (!jsonNode.isNull() && isDocumentField(jsonNode)) {
                String documentId = extractDocumentId(jsonNode);
                if (jsonNode.get(HASH_TOKEN_STRING) != null) {
                    documentMap.put(documentId, jsonNode.get(HASH_TOKEN_STRING).asText());
                } else {
                    documentMap.put(documentId, null);
                }
                ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
            } else {
                if (jsonNode instanceof ArrayNode) {
                    Iterator<JsonNode> arrayNode = ((ArrayNode) jsonNode).elements();
                    while (arrayNode.hasNext()) {
                        JsonNode arrayNodeElement = arrayNode.next();
                        arrayNodeElement.fields().forEachRemaining(node -> extractDocumentIdsAfterCallback(
                            Collections.singletonMap(node.getKey(), node.getValue()), documentMap));

                    }

                } else {
                    jsonNode.fields().forEachRemaining(node -> extractDocumentIdsAfterCallback(
                        Collections.singletonMap(node.getKey(), node.getValue()), documentMap));
                }
            }
        });
    }

    public void restCallToAttachCaseDocuments() {
        if (!caseDocumentsMetadata.getDocumentHashToken().isEmpty()) {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<CaseDocumentsMetadata> requestEntity = new HttpEntity<>(caseDocumentsMetadata, headers);
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

            try {
                restTemplate.exchange(applicationParams.getCaseDocumentAmApiHost().concat(applicationParams.getAttachDocumentPath()),
                    HttpMethod.PATCH, requestEntity, Void.class);

            } catch (HttpClientErrorException restClientException) {
                if (restClientException.getStatusCode() != HttpStatus.FORBIDDEN) {
                    exceptionScenarios(restClientException);
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

    private void exceptionScenarios(HttpClientErrorException restClientException) {
        if (restClientException.getStatusCode() == HttpStatus.BAD_REQUEST) {
            throw new BadSearchRequest(restClientException.getMessage());
        } else if (restClientException.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new ResourceNotFoundException(restClientException.getMessage());
        } else {
            throw new ServiceException("The downstream CCD AM application has failed");
        }
    }

    private String extractDocumentId(JsonNode jsonNode) {
        try {
            String documentId;
            //Document Binary URL is preferred.
            JsonNode documentField = jsonNode.get(DOCUMENT_BINARY_URL) != null
                ? jsonNode.get(DOCUMENT_BINARY_URL) :
                jsonNode.get(DOCUMENT_URL);
            if (documentField.asText().contains(BINARY)) {
                documentId = documentField.asText().substring(documentField.asText().length() - 43, documentField.asText().length() - 7);
            } else {
                documentId = documentField.asText().substring(documentField.asText().length() - 36);
            }
            UUID.fromString(documentId);
            return documentId;
        } catch (RuntimeException e) {
            throw new BadRequestException("The input document id is invalid");
        }
    }


    void consolidateDocumentsWithHashTokenAfterCallBack(CaseDocumentsMetadata caseDocumentsMetadata, Map<String, String> documentsBeforeCallback,
                                                        Map<String, String> documentsAfterCallback) {

        Map<String, String> consolidatedDocumentsWithHashToken;

        if (documentsAfterCallback.size() > 0) {

            // find ids of document inside before call back map which are coming through after call back map
            List<String> commonDocumentIds = documentsAfterCallback.keySet().stream()
                .filter(documentsBeforeCallback::containsKey)
                .collect(Collectors.toList());
            // Check tempered hashToken by call back service
            List<String> temperHashTokenDocumentIds =
                commonDocumentIds.stream()
                    //documentsAfterCallback is a Map and service should not introduce their own hashtoken for a document.
                    .filter(documentId -> StringUtils.isNotEmpty(documentsAfterCallback.get(documentId)))
                    .collect(Collectors.toList());

            if (!temperHashTokenDocumentIds.isEmpty()) {
                throw new ServiceException("call back attempted to change the hashToken of the following documents:" + temperHashTokenDocumentIds);
            }

            //find Hash token of documents which belong to before call back and present in After callback Map
            Map<String, String> commonDocumentIdsWithHashToken = documentsBeforeCallback.entrySet()
                .stream()
                .filter(documentBeforeCallback -> commonDocumentIds.contains(documentBeforeCallback.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            //putting back documentIds with hashToken in after call back map  which belong to before callback Map
            documentsAfterCallback.putAll(commonDocumentIdsWithHashToken);

            // filter after callback  map having hash token
            consolidatedDocumentsWithHashToken = documentsAfterCallback.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            consolidatedDocumentsWithHashToken = documentsBeforeCallback;
        }
        //Filter DocumentHashToken based on consolidatedDocumentsWithHashToken
        consolidatedDocumentsWithHashToken.forEach((key, value) ->
            caseDocumentsMetadata.getDocumentHashToken()
                .add(DocumentHashToken.builder().id(key).hashToken(value).build()));
    }

    Set<String> differenceBeforeAndAfterInCaseDetails(final Map<String, JsonNode> caseDataBefore, final Map<String, JsonNode> caseData) {

        final Map<String, JsonNode> documentsDifference = new HashMap<>();
        final Set<String> filterDocumentSet = new HashSet<>();

        if (null == caseData) {
            return filterDocumentSet;
        }
        //Comparing two jsonNode entities at nested child level
        checkDocumentFieldsDifference(caseDataBefore, caseData, documentsDifference);
        //Find documentId based on filter Map. So that we can filter the DocumentMetaData Object before calling the case document am Api.
        findDocumentsId(documentsDifference, filterDocumentSet);
        return filterDocumentSet;
    }

    private void checkDocumentFieldsDifference(Map<String, JsonNode> caseDataBefore, Map<String, JsonNode> caseData,
                                               Map<String, JsonNode> documentsDifference) {
        caseBeforeNode = null;
        caseData.forEach((key, value) -> {
            if (caseDataBefore.containsKey(key)) {
                extractDocumentFieldsWithDelta(caseDataBefore, documentsDifference, key, value);

            } else if (isDocumentFieldAtAnyLevel(value)) {
                documentsDifference.put(key, value);
            }
        });
    }

    private void extractDocumentFieldsWithDelta(Map<String, JsonNode> caseDataBefore, Map<String, JsonNode> documentsDifference, String key, JsonNode value) {
        caseBeforeNode = caseDataBefore.get(key);
        if (!value.isNull() && isDocumentField(value)) {
            if (!value.equals(caseDataBefore.get(key))) {
                documentsDifference.put(key, value);
            }

        } else {
            checkCaseDetailBeforeNodeType();
            if (value instanceof ArrayNode) {
                Iterator<JsonNode> arrayNode = ((ArrayNode) value).elements();
                while (arrayNode.hasNext()) {
                    JsonNode arrayNodeElement = arrayNode.next();
                    arrayNodeElement.fields().forEachRemaining(node -> checkDocumentFieldsDifference(recursiveMapForCaseDetailsBefore,
                        Collections.singletonMap(node.getKey(), node.getValue()),
                        documentsDifference));

                }

            } else {
                checkNodeTypeAndExtractDocuments(documentsDifference, value);
            }
        }
    }

    private void checkNodeTypeAndExtractDocuments(Map<String, JsonNode> documentsDifference, JsonNode value) {

        if (!value.isNull()) {
            value.fields().forEachRemaining(node -> checkDocumentFieldsDifference(recursiveMapForCaseDetailsBefore,
                Collections.singletonMap(node.getKey(), node.getValue()),
                documentsDifference));
        }
    }

    private void checkCaseDetailBeforeNodeType() {
        if (caseBeforeNode instanceof ArrayNode) {
            Iterator<JsonNode> arrayNode = ((ArrayNode) caseBeforeNode).elements();
            while (arrayNode.hasNext()) {
                JsonNode arrayNodeElement = arrayNode.next();
                Iterator<String> fieldNames = arrayNodeElement.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode before = arrayNodeElement.get(fieldName);
                    recursiveMapForCaseDetailsBefore.put(fieldName, before);
                }

            }
        } else {
            Iterator<String> fieldNames = caseBeforeNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode before = caseBeforeNode.get(fieldName);
                recursiveMapForCaseDetailsBefore.put(fieldName, before);
            }
        }
    }


    public void caseDocumentAttachOperation(CaseDetails caseDetails, CaseDetails caseDetailsBefore, String eventType, boolean callBackWasCalled) {
        extractDocumentsAfterCallBack(caseDetails, callBackWasCalled);
        consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, documentsBeforeCallback, documentsAfterCallback);
        if (eventType.equals(EVENT_UPDATE) || eventType.equals(CMC_EVENT_UPDATE)) {
            //find difference between request payload and existing case detail in db
            final Set<String> filterDocumentSet = differenceBeforeAndAfterInCaseDetails(caseDetailsBefore.getData(), caseDetails.getData());
            //to filter the DocumentMetaData based on filterDocumentSet.
            filterDocumentMetaData(filterDocumentSet);
        }
    }

    private void findDocumentsId(Map<String, JsonNode> sanitisedDataToAttachDoc, Set<String> filterDocumentSet) {

        sanitisedDataToAttachDoc.forEach((field, jsonNode) -> {
            if (!jsonNode.isNull() && isDocumentField(jsonNode)) {
                String documentId = extractDocumentId(jsonNode);
                filterDocumentSet.add(documentId);

            } else {
                if (jsonNode instanceof ArrayNode) {
                    Iterator<JsonNode> arrayNode = ((ArrayNode) jsonNode).elements();
                    while (arrayNode.hasNext()) {
                        JsonNode arrayNodeElement = arrayNode.next();
                        arrayNodeElement.fields().forEachRemaining(node -> findDocumentsId(
                            Collections.singletonMap(node.getKey(), node.getValue()), filterDocumentSet));

                    }

                } else {
                    jsonNode.fields().forEachRemaining(node -> findDocumentsId(
                        Collections.singletonMap(node.getKey(), node.getValue()), filterDocumentSet));
                }
            }
        });
    }

    void filterDocumentMetaData(Set<String> filterDocumentSet) {

        List<DocumentHashToken> caseDocumentList = caseDocumentsMetadata.getDocumentHashToken().stream()
            .filter(document -> filterDocumentSet.contains(document.getId()))
            .collect(Collectors.toList());
        caseDocumentsMetadata.setDocumentHashToken(caseDocumentList);

    }

    private boolean isDocumentFieldAtAnyLevel(JsonNode jsonNode) {
        return jsonNode.findValue(DOCUMENT_BINARY_URL) != null || jsonNode.findValue(DOCUMENT_URL) != null;
    }

    private boolean isDocumentField(JsonNode jsonNode) {
        return jsonNode.has(DOCUMENT_BINARY_URL) || jsonNode.has(DOCUMENT_URL);
    }

    private void existingDocument(Map<String, JsonNode> caseDetailBefore, String newDocumentId) {

        caseDetailBefore.forEach((field, jsonNode) -> {
            if (!jsonNode.isNull() && isDocumentField(jsonNode)) {
                String existingDocumentId = extractDocumentId(jsonNode);
                if (newDocumentId.equalsIgnoreCase(existingDocumentId)) {
                    existingDocumentsInCase.add(newDocumentId);
                }

            } else {
                if (jsonNode instanceof ArrayNode) {
                    Iterator<JsonNode> arrayNode = ((ArrayNode) jsonNode).elements();
                    while (arrayNode.hasNext()) {
                        JsonNode arrayNodeElement = arrayNode.next();
                        arrayNodeElement.fields().forEachRemaining(node -> existingDocument(
                            Collections.singletonMap(node.getKey(), node.getValue()), newDocumentId));

                    }

                } else {
                    jsonNode.fields().forEachRemaining(node -> existingDocument(
                        Collections.singletonMap(node.getKey(), node.getValue()), newDocumentId));
                }
            }
        });

    }

}
