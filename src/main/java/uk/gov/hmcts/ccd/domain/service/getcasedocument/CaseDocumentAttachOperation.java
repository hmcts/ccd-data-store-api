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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.DocumentMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataParsingException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

@Service
public class CaseDocumentAttachOperation {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAttachOperation.class);

    Map<String,String> documentSetBeforeCallback = null;
    Map<String,String> documentAfterCallback = null;
    DocumentMetadata documentMetadata = null;
    public static final String CASE_DATA_PARSING_EXCEPTION = "Exception while extracting the document fields from Case payload";
    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT = "Document";
    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE = "document_binary_url";
    public static final String HASH_CODE_STRING = "hashcode";
    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    public static final String DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION = "The documents have been altered outside the create case transaction";
    public static final String BINARY = "/binary";

    public CaseDocumentAttachOperation(@Qualifier("restTemplate") final RestTemplate restTemplate,
                                       ApplicationParams applicationParams,
                                       SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }

    public void caseDocumentAttachOperation(CaseDetails caseDetails){
        try {
            LOG.debug("Updating  case using Version 2.1 of case create API");
            documentSetBeforeCallback = new HashMap<>();
            extractDocumentFieldsBeforeCallback(caseDetails.getData(), documentSetBeforeCallback);
        }
        catch (Exception e) {
            LOG.error(CASE_DATA_PARSING_EXCEPTION);
            throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
        }
    }

    public void afterCallbackPrepareDocumentMetaData(CaseDetails caseDetails) {
        try {
            documentAfterCallback = new HashMap<>();
            documentMetadata = DocumentMetadata.builder()
                                               .caseId(caseDetails.getReference().toString())
                                               .caseTypeId(caseDetails.getCaseTypeId())
                                               .documentHashToken(new ArrayList<>())
                                               .build();
            // to remove hashcode before compute delta
            extractDocumentFieldsAfterCallback(documentMetadata, caseDetails.getData(), documentAfterCallback);
        } catch (Exception e) {
            LOG.error(CASE_DATA_PARSING_EXCEPTION);
            throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
        }
    }

    public void filterDocumentFields(){
        filterDocumentFields(documentMetadata, documentSetBeforeCallback, documentAfterCallback);
    }

    public void restCallToAttachCaseDocuments(){
        HttpEntity<DocumentMetadata> requestEntity = new HttpEntity<>(documentMetadata, securityUtils.authorizationHeaders());
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        try {
            if (!documentMetadata.getDocumentHashToken().isEmpty()) {
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

    public void extractDocumentFieldsBeforeCallback(Map<String, JsonNode> data, Map<String,String> documentMap) {
        data.forEach((field, jsonNode) -> {
            //Check if the field consists of Document at any level, e.g. Complex fields can also have documents.
            //This quick check will reduce the processing time as most of filtering will be done at top level.
            //****** Every document should have hashcode, else throw error
            if (jsonNode != null && isDocumentField(jsonNode)) {
                if (jsonNode.get(HASH_CODE_STRING) == null) {
                    throw new BadRequestException("The document does not has the hashcode");
                }

                String documentId = extractDocumentId(jsonNode);
                documentMap.put(documentId,jsonNode.get(HASH_CODE_STRING).asText());
                if (jsonNode instanceof ObjectNode) {
                    //((ObjectNode) jsonNode).remove(HASH_CODE_STRING);
                }

            } else {
                jsonNode.fields().forEachRemaining
                    (node -> extractDocumentFieldsBeforeCallback(
                        Collections.singletonMap(node.getKey(), node.getValue()), documentMap));
            }
        });
    }

    public void extractDocumentFieldsAfterCallback(DocumentMetadata documentMetadata, Map<String, JsonNode> data, Map<String,String> documentMap) {
        data.forEach((field, jsonNode) -> {
            //Check if the field consists of Document at any level, e.g. Complex fields can also have documents.
            //This quick check will reduce the processing time as most of filtering will be done at top level.
            //****** Every document should have hashcode, else throw error
            if (jsonNode != null && isDocumentField(jsonNode)) {
                if (jsonNode.get(HASH_CODE_STRING) == null) {
                    throw new BadRequestException("The document does not has the hashcode");
                }
                String documentId = extractDocumentId(jsonNode);
                documentMap.put(documentId,jsonNode.get(HASH_CODE_STRING).asText());
                documentMetadata.getDocumentHashToken().add(DocumentHashToken.builder()
                                                                             .id(documentId)
                                                                             .hashToken(jsonNode.get(HASH_CODE_STRING).asText())
                                                                             .build());

                if (jsonNode instanceof ObjectNode) {
                    ((ObjectNode) jsonNode).remove(HASH_CODE_STRING);
                }

            } else {
                jsonNode.fields().forEachRemaining
                    (node -> extractDocumentFieldsBeforeCallback(
                        Collections.singletonMap(node.getKey(), node.getValue()), documentMap));
            }
        });
    }

    private boolean isDocumentField(JsonNode jsonNode) {
        return jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null
               || jsonNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE) != null;
    }

    public String extractDocumentId(JsonNode jsonNode) {
        JsonNode documentField = jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null ?
                                 jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) :
                                 jsonNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE);

        if (documentField.asText().contains(BINARY)) {
            return documentField.asText().substring(documentField.asText().length() - 43, documentField.asText().length() - 7);
        } else {
            return documentField.asText().substring(documentField.asText().length() - 36);
        }
    }


    private void filterDocumentFields(DocumentMetadata documentMetadata, Map<String,String> documentSetBeforeCallback, Map<String,String> documentSetAfterCallback) {
        try {
            //Below line should be remove before promoting to PR env.
            documentSetAfterCallback.putAll(documentSetBeforeCallback);

            //find documents which are intersection of Before and after callback
            Map<String, String> filteredDocumentSet = documentSetAfterCallback.entrySet().stream()
                                                                              .filter(entry->documentSetBeforeCallback.containsKey(entry.getKey()) && documentSetBeforeCallback.get(entry.getKey()).equals(entry.getValue()))
                                                                              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            //Add the intersection to aftercallback list. Now, afterCallbackList will have the documents from
            //Callback response
            // + original documents which have not been removed by the callback
            // + Any new documents which are added by callback response
            //This code should drop any documents which were removed by the callback
            documentSetAfterCallback.putAll(filteredDocumentSet);
            List<DocumentHashToken> filterCaseDocument = documentMetadata.getDocumentHashToken()
                                                                    .stream()
                                                                    .filter(caseDocument -> documentSetAfterCallback
                                                                                                .containsKey(caseDocument.getId()) && documentSetAfterCallback
                                                                                                .get(caseDocument.getId())
                                                                                                .equals(caseDocument.getHashToken()))
                                                                    .collect(Collectors.toList());
            documentMetadata.setDocumentHashToken(filterCaseDocument);
        } catch (Exception e) {
            LOG.error("Exception while filtering the document fields.");
            throw new DataParsingException("Exception while filtering the document fields.");
        }
    }
    public Set<String> differenceBeforeAndAfterInCaseDetails(final CaseDetails caseDetails, final Map<String, JsonNode> caseData) {

        final Map<String, JsonNode> documentsDifference = new HashMap<>();
        final Set<String> filterDocumentSet = new HashSet<>();

        if (null == caseData) {
            return filterDocumentSet;
        }

        caseData.forEach((key, value) -> {

            if ((value.findValue(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null || value.findValue(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE) != null) && caseDetails.getData().containsKey(key)) {
                if(!value.equals(caseDetails.getData().get(key)))
                {
                    documentsDifference.put(key,value);
                }
            } else if (value.findValue(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null || value.findValue(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE) != null){
                documentsDifference.put(key,value);
            }
        });
        selectDocument(documentsDifference,filterDocumentSet);
        return filterDocumentSet;
    }
    private void selectDocument(Map<String, JsonNode> sanitisedDataToAttachDoc, Set<String> filterDocumentSet) {

        sanitisedDataToAttachDoc.forEach((field, jsonNodeValue) -> {
            if(jsonNodeValue !=null && (jsonNodeValue.findValue(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE )!= null || jsonNodeValue.findValue(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null)){

                JsonNode documentBinaryField = jsonNodeValue.findValue(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE);
                if (documentBinaryField != null) {
                    filterDocumentSet.add(documentBinaryField.asText().substring(documentBinaryField.asText().length() - 43, documentBinaryField.asText().length() - 7));

                } else {
                    JsonNode documentField = jsonNodeValue.findValue(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE);
                    filterDocumentSet.add(documentField.asText().substring(documentField.asText().length() - 36));
                }
            }

        });


    }

    public void filterDocumentMetaData(Set<String> filterDocumentSet){

        List<DocumentHashToken> caseDocumentList = documentMetadata.getDocumentHashToken().stream()
                                                              .filter(document -> filterDocumentSet.contains(document.getId()))
                                                              .collect(Collectors.toList());
        documentMetadata.setDocumentHashToken(caseDocumentList);

    }



}
