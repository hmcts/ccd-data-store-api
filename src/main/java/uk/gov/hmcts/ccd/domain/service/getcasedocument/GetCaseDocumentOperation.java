package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
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
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GetCaseDocumentOperation {

    private final GetCaseOperation getCaseOperation;
    private final CaseTypeService caseTypeService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;
    private final DocumentIdValidationService documentIdValidationService;

    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_NAME_ATTRIBUTE = "document_filename";
    public static final String DOCUMENT_CASE_FIELD_TYPE_ATTRIBUTE = "Document";
    public static final String BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID = "DocumentId is not valid";

    @Autowired
    public GetCaseDocumentOperation(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        final CaseTypeService caseTypeService,
        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
        @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
        DocumentIdValidationService documentIdValidationService) {

        this.getCaseOperation = getCaseOperation;
        this.caseTypeService = caseTypeService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
        this.documentIdValidationService = documentIdValidationService;
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

        //retrieve the case data from details
        Map<String, JsonNode> caseData = caseDetails.getData();

        //get document field name
        String documentField = getDocumentCaseField(caseData, documentId);

        //get child fields and set to caseDocument object
        if (documentField != null) {

            Optional<AccessControlList> userACLOnCaseField = getCaseFieldACLByUserRoles(caseDetails, documentField);
            if (userACLOnCaseField.isPresent()) {

                //build caseDocument and set permissions
                return CaseDocument.builder()
                    .id(documentId)
                    .url(caseData.get(documentField).get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE).asText())
                    .name(caseData.get(documentField).get(DOCUMENT_CASE_FIELD_NAME_ATTRIBUTE).asText())
                    .type(DOCUMENT_CASE_FIELD_TYPE_ATTRIBUTE)
                    .permissions(getDocumentPermissions(userACLOnCaseField.get()))
                    .build();
            } else {
                throw new CaseDocumentNotFoundException(
                    String.format("No document found for this case reference: %s",
                        caseDetails.getReferenceAsString()));
            }

        } else {
            throw new CaseDocumentNotFoundException(
                String.format("No document found for this case reference: %s", caseDetails.getReferenceAsString()));
        }
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
            if (!entry.getValue().isNull()){
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
