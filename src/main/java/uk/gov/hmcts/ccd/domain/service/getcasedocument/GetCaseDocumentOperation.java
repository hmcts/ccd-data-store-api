package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
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

        if (caseDetails.getReferenceAsString().isEmpty()) {
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
        List<CaseField> documentCaseFields = new ArrayList<>();
        //Step2
        // Collection Case fields having collection field type as document.

        //If this list contains collection casefield again then repeat the above step to extract the full list of document casefields.
        //finalDocumentCaseFields.addAll(collectionCaseFields);
        //<more logic to come>

        List<CaseField> complexCaseFieldList = caseType.getCaseFields()
            .stream()
            .filter(y -> ("Document" .equalsIgnoreCase(y.getFieldType().getType())) ||
                    ("Complex".equalsIgnoreCase(y.getFieldType().getType())) ||
                    ("Collection".equalsIgnoreCase(y.getFieldType().getType())))
            .collect(Collectors.toList());

        if (complexCaseFieldList.isEmpty()) {
            throw new CaseDocumentNotFoundException(String.format("No document field found for CaseType : %s", caseType.getId()));
        }

        extractDocumentFields(complexCaseFieldList, documentCaseFields);

        JsonNode fieldsWithReadPermission = getFieldsWithReadPermission(caseDetails, documentCaseFields)
            .orElseThrow(
                (() -> new CaseDocumentNotFoundException("User does not has read permissions on any document field")));

        String documentNode = getDocumentFieldNode(documentId, fieldsWithReadPermission);

        if (!StringUtils.isEmpty(documentNode)) {
            //build caseDocument and set permissions
            return CaseDocument.builder()
                .id(documentId)
                .url(documentNode)
                //.name(documentNode.get(DOCUMENT_CASE_FIELD_NAME_ATTRIBUTE).asText())
                //.type(DOCUMENT_CASE_FIELD_TYPE_ATTRIBUTE)
                .permissions(Arrays.asList(Permission.READ))
                .build();
        } else {
            throw new CaseDocumentNotFoundException(
                String.format("No document found for this case reference: %s",
                    caseDetails.getReferenceAsString()));
        }
    }

    private String getDocumentFieldNode(String documentId, JsonNode readPermission) {
        String urlPatternString = "^(?:\\/\\/|[^\\/]+)*\\/documents\\/[a-zA-Z0-9-]{36}";

        Pattern pattern = Pattern.compile(urlPatternString);
        for (JsonNode jsonNode : readPermission) {
            //Find thr document ID within the caseField JSON Node
            //1. Match from a. URL key or b. description key
            //We need to validate whether documentId is part of URL pattern and key of that node contains "url" word.

            Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                if (entry.getValue().asText().contains(documentId)
                    && entry.getKey().toUpperCase().contains("URL")
                    && pattern.matcher(entry.getValue().asText()).matches()) {
                    return entry.getValue().asText();
                }
            }
        }
        return null;
    }

    private void extractDocumentFields(List<CaseField> complexCaseFieldList, List<CaseField> documentCaseFields) {
        for (CaseField caseField : complexCaseFieldList) {
            switch (caseField.getFieldType().getType()) {
                case "Document":
                    documentCaseFields.add(caseField);
                    break;
                case "Complex":
                case "Collection":
                    extractDocumentFields(caseField.getFieldType().getComplexFields(), documentCaseFields);
                    break;
            }
        }
    }

    private Optional<JsonNode> getFieldsWithReadPermission(CaseDetails caseDetails, List<CaseField> documentFields) {
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
