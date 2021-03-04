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
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT = "Document";
    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE = "document_binary_url";
    public static final String BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID = "DocumentId is not valid";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public GetCaseDocumentOperation(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        final CaseTypeService caseTypeService,
        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
        @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
        DocumentIdValidationService documentIdValidationService,
        AccessControlService accessControlService) {

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

        if (caseDetails.getReferenceAsString() == null) {
            throw new CaseNotFoundException(caseId);
        }

        return CaseDocumentMetadata.builder()
            .caseId(caseDetails.getReferenceAsString())
            .documentPermissions(getCaseDocument(caseDetails, documentId))
            .build();

    }

    private DocumentPermissions getCaseDocument(CaseDetails caseDetails, String documentId) {

        String caseTypeId = caseDetails.getCaseTypeId();
        String jurisdictionId = caseDetails.getJurisdiction();
        final CaseTypeDefinition caseType = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);

        List<CaseFieldDefinition> documentCaseFields = new ArrayList<>();
        List<CaseFieldDefinition> documentAndComplexFields = caseType.getCaseFieldDefinitions()
            .stream()
            .filter(caseField -> (DOCUMENT.equalsIgnoreCase(caseField.getFieldTypeDefinition().getType()))
                    || (COMPLEX.equalsIgnoreCase(caseField.getFieldTypeDefinition().getType()))
                    || (COLLECTION.equalsIgnoreCase(caseField.getFieldTypeDefinition().getType())))
            .collect(Collectors.toList());

        if (documentAndComplexFields.isEmpty()) {
            throw new CaseDocumentNotFoundException(
                String.format("No document field found for CaseType : %s", caseType.getId()));
        }

        extractDocumentFieldsFromCaseDefinition(documentAndComplexFields, documentCaseFields);
        JsonNode documentFieldsWithReadPermission = getDocumentFieldsWithReadPermission(caseDetails, documentCaseFields)
            .orElseThrow((() ->
                new CaseDocumentNotFoundException("User does not has read permissions on any document field")));


        if (Boolean.TRUE.equals(isDocumentPresent(documentId, documentFieldsWithReadPermission))) {
            //build caseDocument and set permissions
            return DocumentPermissions.builder()
                .id(documentId)
                .permissions(Arrays.asList(Permission.READ))
                .build();
        } else {
            throw new CaseDocumentNotFoundException(
                String.format("No document found for this case reference: %s",
                    caseDetails.getReferenceAsString()));
        }
    }

    private Boolean isDocumentPresent(String documentId, JsonNode documentFieldsWithReadPermission) {
        for (JsonNode jsonNode : documentFieldsWithReadPermission) {
            if ((jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE) != null
                && jsonNode.get(DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE).asText().contains(documentId))
                || (jsonNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE) != null
                && jsonNode.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE).asText().contains(documentId))) {
                return true;
            }
        }
        return false;
    }

    void extractDocumentFieldsFromCaseDefinition(List<CaseFieldDefinition> complexCaseFieldList,
                                                  List<CaseFieldDefinition> documentCaseFields) {
        if (complexCaseFieldList != null && !complexCaseFieldList.isEmpty()) {
            for (CaseFieldDefinition caseField : complexCaseFieldList) {
                getDocumentFields(documentCaseFields, caseField);
            }
        }
    }

    private void getDocumentFields(List<CaseFieldDefinition> documentCaseFields, CaseFieldDefinition caseField) {
        switch (caseField.getFieldTypeDefinition().getType()) {
            case DOCUMENT:
                documentCaseFields.add(caseField);
                break;
            case COMPLEX:
            case COLLECTION:
                if (caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition() != null) {
                    if (caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields()
                            != null) {
                        extractDocumentFieldsFromCaseDefinition(
                            caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields(),
                            documentCaseFields);
                    }
                    if (caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition()
                            .getCollectionFieldTypeDefinition() != null) {
                        extractDocumentFieldsFromCaseDefinition(
                            caseField.getFieldTypeDefinition().getCollectionFieldTypeDefinition()
                                .getCollectionFieldTypeDefinition().getComplexFields(), documentCaseFields);
                    }
                }
                extractDocumentFieldsFromCaseDefinition(caseField.getFieldTypeDefinition().getComplexFields(),
                                                        documentCaseFields);
                break;
            default:
                break;
        }
    }

    private Optional<JsonNode> getDocumentFieldsWithReadPermission(CaseDetails caseDetails,
                                                                   List<CaseFieldDefinition> documentFields) {
        Set<String> roles = getUserRoles(caseDetails.getId());
        return Optional.of(accessControlService.filterCaseFieldsByAccess(
            MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
            documentFields,
            roles,
            CAN_READ, true));
    }

    private Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            new HashSet<>(caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())));
    }
}
