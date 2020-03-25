package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
public class GetCaseDocumentOperation {

    private final GetCaseOperation getCaseOperation;
    private final CaseTypeService caseTypeService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;
    private final DocumentIdValidationService documentIdValidationService;
    private final AccessControlService accessControlService;

    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_NAME_ATTRIBUTE = "document_filename";
    public static final String DOCUMENT_CASE_FIELD_TYPE_ATTRIBUTE = "Document";
    public static final String BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID = "DocumentId is not valid";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public GetCaseDocumentOperation(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        final CaseTypeService caseTypeService,
        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
        @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
        DocumentIdValidationService documentIdValidationService
        , AccessControlService accessControlService) {

        this.getCaseOperation = getCaseOperation;
        this.caseTypeService = caseTypeService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
        this.documentIdValidationService = documentIdValidationService;
        this.accessControlService = accessControlService;
    }


    public CaseDocumentMetadata getCaseDocumentMetadata(String caseId, String documentId) {

        if (!documentIdValidationService.validateDocumentUUID(documentId)) {
            throw new BadRequestException(BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID);
        }

        final CaseDetails caseDetails = this.getCaseOperation.execute(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));

        if (caseDetails.getReferenceAsString().isEmpty()){
            throw new CaseNotFoundException(caseId);
        }

        return CaseDocumentMetadata.builder()
            .caseId(caseDetails.getReferenceAsString())
            .caseTypeId(caseDetails.getCaseTypeId())
            .jurisdictionId(caseDetails.getJurisdiction())
            .document(getCaseDocument(caseDetails, documentId))
            .build();

    }

    public CaseDocument getCaseDocument(CaseDetails caseDetails, String documentId) {

        //Retrieve the caseType and list of casefield containing document casefield
        //get the caseTypeId and JID & corresponding casetype
        String caseTypeId = caseDetails.getCaseTypeId();
        String jurisdictionId = caseDetails.getJurisdiction();
        final CaseType caseType = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
        //Step1
        //Extract the list of complex case Fields having document casefields or comlex casefields
        List<CaseField> finalDocumentCaseFields = new ArrayList<>();
        //Step2
        // Collection Case fields having collection field type as document.

        //If this list contains collection casefield again then repeat the above step to extract the full list of document casefields.
        //finalDocumentCaseFields.addAll(collectionCaseFields);
        //<more logic to come>

        List<CaseField> complexCaseFieldList = caseType.getCaseFields()
            .stream()
            .filter
                (y -> ("Document".equalsIgnoreCase(y.getFieldType().getType())) ||
                ("Complex".equalsIgnoreCase(y.getFieldType().getType())) ||
                ("Collection".equalsIgnoreCase(y.getFieldType().getType())))
            .collect(Collectors.toList());

        extractDocumentFields(complexCaseFieldList, finalDocumentCaseFields);

        //Retrieve the full list of available JSON node on which user is having read permissions (as per get case API).  ListB

        JsonNode readPermission = getFieldsWithReadPermission(caseDetails, finalDocumentCaseFields);
        JsonNode documentNode = getDocumentFieldNode(documentId, readPermission);

        //get child fields and set to caseDocument object
        if (documentNode != null) {
            //build caseDocument and set permissions
            return CaseDocument.builder()
                .id(documentId)
                .url(documentNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE).asText())
                .name(documentNode.get(DOCUMENT_CASE_FIELD_NAME_ATTRIBUTE).asText())
                .type(documentNode.get(DOCUMENT_CASE_FIELD_TYPE_ATTRIBUTE).asText())
                .permissions(Arrays.asList(Permission.READ))
                .build();
        } else {
            throw new CaseDocumentNotFoundException(
                String.format("No document found for this case reference: %s",
                    caseDetails.getReferenceAsString()));
        }
    }

    private JsonNode getDocumentFieldNode(String documentId, JsonNode readPermission) {
        for (JsonNode jsonNode : readPermission) {
            JsonNode textNode = jsonNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE);
            if (textNode.asText().substring(textNode.asText().length() - 36).equals(documentId)) {
                return jsonNode;
            }
        }
        return null;
    }

    private void extractDocumentFields(List<CaseField> complexCaseFieldList2, List<CaseField> finalDocumentCaseFields) {
        for (CaseField caseField : complexCaseFieldList2) {
            switch (caseField.getFieldType().getType()) {
                case "Document":
                    finalDocumentCaseFields.add(caseField);
                    break;
                case "Complex":
                    extractDocumentFields(caseField.getFieldType().getComplexFields(), finalDocumentCaseFields);
                    break;
                case "Collection":
                    extractDocumentFields(caseField.getFieldType().getComplexFields(), finalDocumentCaseFields);
                    break;
            }
        }
    }

    private JsonNode getFieldsWithReadPermission(CaseDetails caseDetails, List<CaseField> documentFields) {
        Set<String> roles = getUserRoles(caseDetails.getId());

        return accessControlService.filterCaseFieldsByAccess(
            MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
            documentFields,
            roles,
            CAN_READ,
            true);
    }

    private Optional<AccessControlList> getCaseFieldACLByUserRoles(CaseDetails caseDetails, String documentField) {
        //get the caseTypeId and JID
        String caseTypeId = caseDetails.getCaseTypeId();
        String jurisdictionId = caseDetails.getJurisdiction();
        //retrieve the caseType and the documentCaseField
        final CaseType caseType = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
        final Optional<CaseField> documentCaseField = caseType.getCaseField(documentField);
        //get the users role
        Set<String> roles = getUserRoles(caseDetails.getId());

        //Check the documentField found
        if (documentCaseField.isPresent()) {
            if (!roles.isEmpty()) {
                //retrieve all ACL on the user roles
                Optional<AccessControlList> userACLOnCaseField = Optional.empty();
                for (String role : roles) {
                    userACLOnCaseField = documentCaseField.get().getAccessControlLists()
                        .stream()
                        .filter(acl -> acl.getRole().equalsIgnoreCase(role))
                        .findFirst();
                    if (userACLOnCaseField.isPresent()) {
                        return userACLOnCaseField;
                    }
                }
                return userACLOnCaseField;
            } else {
                throw new CaseDocumentNotFoundException(
                    String.format("No valid user role found for this case reference: %s",
                        caseDetails.getReferenceAsString()));
            }
        } else {
            throw new CaseDocumentNotFoundException(
                String.format("No document found for this case reference: %s", caseDetails.getReferenceAsString()));
        }
    }

    private Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            new HashSet<>(caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())));
    }

    private String getDocumentCaseField(Map<String, JsonNode> caseData, String documentId) {
        for (Map.Entry<String, JsonNode> entry : caseData.entrySet()) {
            if (entry.getValue().isNull()){
                return null;
            }
            if (entry.getValue().getNodeType().toString()
                .equals("OBJECT") && entry.getValue().toString().contains(documentId)) {
                return entry.getKey();
            }
        }
        return null;
    }


    private List<Permission> getDocumentPermissions(AccessControlList userACL) {
        final List<Permission> permissions = new ArrayList<>();

        if (userACL.isRead()) {
            permissions.add(Permission.READ);
        }

        if (userACL.isUpdate()) {
            permissions.add(Permission.UPDATE);
        }

        if (userACL.isCreate()) {
            permissions.add(Permission.CREATE);
        }

        if (userACL.isDelete()) {
            permissions.add(Permission.DELETE);
        }
        return permissions;
    }

}
