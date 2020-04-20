package uk.gov.hmcts.ccd.domain.service.getcasedocument;

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
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class CaseDocumentAttacher {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAttacher.class);

    Map<String, String> documentsBeforeCallback = null;
    Map<String, String> documentsAfterCallback = null;
    Map<String, String> documentAfterCallbackOriginalCopy = new HashMap<>();
    CaseDocumentsMetadata caseDocumentsMetadata = null;
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
    public static final String EVENT_TYPE = "UPDATE";

    public CaseDocumentAttacher( RestTemplate restTemplate,
                                 ApplicationParams applicationParams,
                                 SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }

    public void caseDocumentAttacherOperations(CaseDetails caseDetails, CaseDetails caseDetailsBefore,String eventType, boolean callBackWasCalled){
        extractDocumentsAfterCallBack(caseDetails,callBackWasCalled);
        consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, documentsBeforeCallback, documentsAfterCallback);
        if(eventType.equals(EVENT_TYPE)) {
            //find difference between request payload and existing case detail in db
            final Set<String> filterDocumentSet = differenceBeforeAndAfterInCaseDetails(caseDetailsBefore, caseDetails.getData());
            //to filter the DocumentMetaData based on filterDocumentSet.
            filterDocumentMetaData(filterDocumentSet);
        }
    }

    public void extractDocumentsWithHashTokenBeforeCallback(Map<String,JsonNode> contentData){

        LOG.debug("Updating  case using Version 2.1 of case create API");
        documentsBeforeCallback = new HashMap<>();
        extractDocumentsWithHashTokenBeforeCallback(contentData, documentsBeforeCallback);

    }

    public  void extractDocumentsAfterCallBack(CaseDetails caseDetails, boolean callBackWasCalled){

        documentsAfterCallback = new HashMap<>();
        caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                     .caseId(caseDetails.getReference().toString())
                                                     .caseTypeId(caseDetails.getCaseTypeId())
                                                     .jurisdictionId(caseDetails.getJurisdiction())
                                                     .documentHashToken(new ArrayList<>())
                                                     .build();

        if(callBackWasCalled) {
            // to remove hashcode before compute delta
            extractDocumentsAfterCallback(caseDocumentsMetadata, caseDetails.getData(), documentsAfterCallback);
        }

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
                //Revisit this piece of code again.
                if (restClientException.getStatusCode() != HttpStatus.FORBIDDEN) {
                    throw new BadSearchRequest(restClientException.getMessage());
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

    public void extractDocumentsWithHashTokenBeforeCallback(Map<String, JsonNode> data, Map<String,String> documentMap) {
        data.forEach((field, jsonNode) -> {
            if (jsonNode != null && isDocumentField(jsonNode))  {
                if (jsonNode.get(HASH_TOKEN_STRING) == null) {
                    throw new BadRequestException("The document does not has the hashcode");
                }
                String documentId = extractDocumentId(jsonNode);
                documentMap.put(documentId,jsonNode.get(HASH_TOKEN_STRING).asText());
                if (jsonNode instanceof ObjectNode) {
                    ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
                }

            } else {
                jsonNode.fields().forEachRemaining
                    (node -> extractDocumentsWithHashTokenBeforeCallback(
                        Collections.singletonMap(node.getKey(), node.getValue()), documentMap));
            }
        });
    }

    public void extractDocumentsAfterCallback(CaseDocumentsMetadata caseDocumentsMetadata, Map<String, JsonNode> data, Map<String,String> documentMap) {
        data.forEach((field, jsonNode) -> {
            if (jsonNode != null && isDocumentField(jsonNode)){

                String documentId = extractDocumentId(jsonNode);
                documentMap.put(documentId,jsonNode.get(HASH_TOKEN_STRING).asText());
                if (jsonNode instanceof ObjectNode) {
                    ((ObjectNode) jsonNode).remove(HASH_TOKEN_STRING);
                }

            } else {
                jsonNode.fields().forEachRemaining
                    (node -> extractDocumentsAfterCallback(caseDocumentsMetadata,
                                                           Collections.singletonMap(node.getKey(), node.getValue()), documentMap));
            }
        });

    }


    private boolean isDocumentField(JsonNode jsonNode) {
        return jsonNode.get(DOCUMENT_BINARY_URL) != null
               || jsonNode.get(DOCUMENT_URL) != null;
    }
    public String extractDocumentId(JsonNode jsonNode) {
        //Document Binary URL is preferred.
        JsonNode documentField = jsonNode.get(DOCUMENT_BINARY_URL) != null ?
                                 jsonNode.get(DOCUMENT_BINARY_URL) :
                                 jsonNode.get(DOCUMENT_URL);
        if (documentField.asText().contains(BINARY)) {
            return documentField.asText().substring(documentField.asText().length() - 43, documentField.asText().length() - 7);
        } else {
            return documentField.asText().substring(documentField.asText().length() - 36);
        }
    }


    public void consolidateDocumentsWithHashTokenAfterCallBack(CaseDocumentsMetadata caseDocumentsMetadata, Map<String,String> documentsBeforeCallback, Map<String,String> documentsAfterCallback) {

        Map<String,String> consolidatedDocumentsWithHashToken;

        if(documentsAfterCallback.size()>0) {

            // find ids of document inside before call back map which are coming through after call back map
            List<String> commonDocumentIds=documentsAfterCallback.keySet().stream().filter(str->documentsBeforeCallback.containsKey(str)).collect(Collectors.toList());

            //find Hash token of documents belong to  before call back which are in After callback Map
            Map<String,String> commonDocumentIdsWithHashToken = documentsBeforeCallback.entrySet()
                                                                                       .stream()
                                                                                       .filter(e -> commonDocumentIds.contains(e.getKey()))
                                                                                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Check temper hashToken by call back service
            List<String> temperHashTokenDocumentIds =
                commonDocumentIds.stream()
                                 .map(documentId -> {
                                     if(documentsAfterCallback.get(documentId) != null ) {
                                         return documentId;
                                     }
                                     return "";
                                 }).collect(Collectors.toList());

            //remove empty string from list
            temperHashTokenDocumentIds.removeIf(String::isEmpty);

            if(!temperHashTokenDocumentIds.isEmpty()){
                throw new ServiceException("call back attempted to change the hashToken of the following documents:" + temperHashTokenDocumentIds);
            }


            //putting back documentIds with hashToken in after call back map  which belong to before callback Map
            documentsAfterCallback.putAll(commonDocumentIdsWithHashToken);

            // filter after callback  map having hash token
            consolidatedDocumentsWithHashToken = documentsAfterCallback.entrySet().stream().filter(e->e.getValue()!=null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));



        }else {
            consolidatedDocumentsWithHashToken = documentsBeforeCallback;

        }
        //Filter DocumentHashToken based on consolidatedDocumentsWithHashToken
        consolidatedDocumentsWithHashToken.forEach((key, value) -> {
            caseDocumentsMetadata.getDocumentHashToken().add(DocumentHashToken
                                                         .builder()
                                                         .id(key)
                                                         .hashToken(value)
                                                         .build());
        });


    }
    public Set<String> differenceBeforeAndAfterInCaseDetails(final CaseDetails caseDetails, final Map<String, JsonNode> caseData) {

        final Map<String, JsonNode> documentsDifference = new HashMap<>();
        final Set<String> filterDocumentSet = new HashSet<>();

        if (null == caseData) {
            return filterDocumentSet;
        }

        caseData.forEach((key, value) -> {

            if (caseDetails.getData().containsKey(key) && isDocumentFieldAtAnyLevel(value)) {
                if(!value.equals(caseDetails.getData().get(key)))
                {
                    documentsDifference.put(key,value);
                }
            } else if(isDocumentFieldAtAnyLevel(value)){
                documentsDifference.put(key,value);
            }
        });
        //Find documentId based on filter Map. So that I can filter the DocumentMetaData Object before calling the case document am Api.
        findDocumentsId(documentsDifference,filterDocumentSet);
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
                jsonNode.fields().forEachRemaining
                    (node -> findDocumentsId(
                        Collections.singletonMap(node.getKey(), node.getValue()), filterDocumentSet));
            }
        });



    }

    public void filterDocumentMetaData(Set<String> filterDocumentSet){

        List<DocumentHashToken> caseDocumentList = caseDocumentsMetadata.getDocumentHashToken().stream()
                                                                        .filter(document -> filterDocumentSet.contains(document.getId()))
                                                                        .collect(Collectors.toList());
        caseDocumentsMetadata.setDocumentHashToken(caseDocumentList);

    }

    private boolean isDocumentFieldAtAnyLevel(JsonNode jsonNode) {
        return jsonNode.findValue(DOCUMENT_BINARY_URL) != null || jsonNode.findValue(DOCUMENT_URL) != null;
    }



}
