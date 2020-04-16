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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataParsingException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

@Service
public class CaseDocumentAttachOperation {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAttachOperation.class);

    Map<String, String> documentSetBeforeCallback = null;
    Map<String, String> documentAfterCallback = null;
    CaseDocumentsMetadata caseDocumentsMetadata = null;
    public static final String CASE_DATA_PARSING_EXCEPTION = "Exception while extracting the document fields from Case payload";
    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT = "Document";
    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE = "document_binary_url";
    public static final String HASH_TOKEN_STRING = "hashToken";

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

    public void beforeCallbackPrepareDocumentMetaData(Map<String, JsonNode> caseData) {
        documentSetBeforeCallback = new HashMap<>();
        extractDocumentFieldsBeforeCallback(caseData, documentSetBeforeCallback);
    }

    public void beforeCallbackPrepareDocumentMetaData(CaseDataContent contentData) {
        try {
            LOG.debug("Updating  case using Version 2.1 of case create API");
            documentSetBeforeCallback = new HashMap<>();
            extractDocumentFieldsBeforeCallback(contentData.getData(), documentSetBeforeCallback);
        } catch (Exception e) {
            LOG.error(CASE_DATA_PARSING_EXCEPTION);
            throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
        }
    }

    public void afterCallbackPrepareDocumentMetaData(CaseDetails caseDetails, boolean callBackResult) {
        try {
            documentAfterCallback = new HashMap<>();
            caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                         .caseId(caseDetails.getReference().toString())
                                                         .caseTypeId(caseDetails.getCaseTypeId())
                                                         .jurisdictionId(caseDetails.getJurisdiction())
                                                         .documentHashToken(new ArrayList<>())
                                                         .build();

            if (callBackResult)
            // to remove hashcode before compute delta
            {
                extractDocumentFieldsAfterCallback(caseDocumentsMetadata, caseDetails.getData(), documentAfterCallback);
            }
        } catch (Exception e) {
            LOG.error(CASE_DATA_PARSING_EXCEPTION);
            throw new DataParsingException(CASE_DATA_PARSING_EXCEPTION);
        }
    }

    public void filterDocumentFields() {
        filterDocumentFields(caseDocumentsMetadata, documentSetBeforeCallback, documentAfterCallback);
    }

    public void restCallToAttachCaseDocuments() {
        HttpEntity<CaseDocumentsMetadata> requestEntity = new HttpEntity<>(caseDocumentsMetadata, securityUtils.authorizationHeaders());
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        if (!caseDocumentsMetadata.getDocumentHashToken().isEmpty()) {
            restTemplate
                .exchange(applicationParams.getCaseDocumentAmApiHost().concat(applicationParams.getAttachDocumentPath()),
                          HttpMethod.PATCH, requestEntity, Void.class);

        }
    }

    public void extractDocumentFieldsBeforeCallback(Map<String, JsonNode> data, Map<String, String> documentMap) {
        data.forEach((field, jsonNode) -> {
            if (jsonNode != null && isDocumentField(jsonNode)) {
                if (jsonNode.get(HASH_TOKEN_STRING) == null) {
                    throw new BadRequestException("The document does not has the hashToken");
                }

                String documentId = extractDocumentId(jsonNode);
                documentMap.put(documentId, jsonNode.get(HASH_TOKEN_STRING).asText());
                if (jsonNode instanceof ObjectNode) {
                    ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
                }
            } else {
                jsonNode.fields().forEachRemaining(node -> extractDocumentFieldsBeforeCallback(
                    Collections.singletonMap(node.getKey(), node.getValue()), documentMap));
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


    public void filterDocumentFields(CaseDocumentsMetadata caseDocumentsMetadata, Map<String, String> documentSetBeforeCallback,
                                     Map<String, String> documentSetAfterCallback) {
        try {

            if (documentSetAfterCallback.size() > 0) {
                // find ids of document inside before call back map which are coming through after call back map
                List<String> commonDocumentIds = documentSetAfterCallback.keySet().stream().filter(documentSetBeforeCallback::containsKey)
                                                                         .collect(Collectors.toList());

                //find Hash token of documents belong to  before call back which are in After callback Map
                Map<String, String> beforeCallBackFilterMap = documentSetBeforeCallback.entrySet()
                                                                                       .stream()
                                                                                       .filter(e -> commonDocumentIds.contains(e.getKey()))
                                                                                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                //putting back documentIds with hashToken in after call back map  which belong to before callback Map
                documentSetAfterCallback.putAll(beforeCallBackFilterMap);

                //  filter after callback  map having hash token
                Map<String, String> finalMap = documentSetAfterCallback.entrySet().stream().filter(e -> e.getValue() != null)
                                                                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                //Filter DocumentHashToken based on finalMap
                finalMap.forEach((key, value) ->
                                     caseDocumentsMetadata.getDocumentHashToken().add(DocumentHashToken
                                                                                          .builder()
                                                                                          .id(key)
                                                                                          .hashToken(value)
                                                                                          .build()));
            } else {
                documentSetBeforeCallback.forEach((key, value) ->
                                                      caseDocumentsMetadata.getDocumentHashToken().add(DocumentHashToken
                                                                                                           .builder()
                                                                                                           .id(key)
                                                                                                           .hashToken(value)
                                                                                                           .build())
                                                 );

            }
        } catch (Exception e) {
            LOG.error("Exception while filtering the document fields.");
            throw new DataParsingException("Exception while filtering the document fields.");
        }

    }
}
