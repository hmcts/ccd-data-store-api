package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.GetCaseDocumentOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDocumentResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/cases")
public class CaseDocumentController {

    private final UIDService caseReferenceService;
    private final GetCaseOperation getCaseOperation;
    private final CaseTypeService caseTypeService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;
    private final GetCaseDocumentOperation getCaseDocumentOperation;

    @Autowired
    public CaseDocumentController(final UIDService caseReferenceService,
                                  @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                                  final CaseTypeService caseTypeService,
                                  @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                  @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
                                  @Qualifier("default") GetCaseDocumentOperation getCaseDocumentOperation) {

        this.caseReferenceService = caseReferenceService;
        this.getCaseOperation = getCaseOperation;
        this.caseTypeService = caseTypeService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
        this.getCaseDocumentOperation = getCaseDocumentOperation;
    }

    @GetMapping(
        path = "/{caseId}/documents/{documentId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_DOCUMENTS
        }
    )
    @ApiOperation(
        value = "Retrieve a case document metadata by case and document Id",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseDocumentResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
        )
    })
    public ResponseEntity<CaseDocumentResource> getCaseDocumentMetadata(@PathVariable("caseId") String caseId,
                                                                        @PathVariable("documentId") String documentId) {

        final CaseDetails caseDetails = this.getCaseOperation.execute(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        final CaseDocument caseDocument = this.getCaseDocument(caseDetails, documentId);
        final CaseDocumentMetadata documentMetadata = this.prepareDocumentMetadata(caseDetails, caseDocument);
        return ResponseEntity.ok(new CaseDocumentResource(caseId, documentId, documentMetadata));
    }

    private CaseDocument getCaseDocument(CaseDetails caseDetails, String documentId) {

        //validate the document UUID
        try {
            UUID id = UUID.fromString(documentId);
            if (!id.toString().equals(documentId)) {
                throw new BadRequestException("DocumentId is not valid");
            }
        } catch (IllegalArgumentException exception) {
            //handle the case where string is not valid UUID
            throw new BadRequestException("DocumentId is not valid");
        }


        //retrieve the case data from details
        Map<String, JsonNode> caseData = caseDetails.getData();
        //create caseDocument and set documentId
        CaseDocument caseDocument = new CaseDocument();
        caseDocument.setId(documentId);
        //get document field name
        //Optional<String> documentField = Optional.ofNullable(getDocumentCaseField(caseData, documentId));
        String documentField = getDocumentCaseField(caseData, documentId);
        //get child fields and set to caseDocument object

        if (documentField != null) {
            caseDocument.setUrl(caseData.get(documentField).get("document_url").asText());
            caseDocument.setName(caseData.get(documentField).get("document_filename").asText());
            caseDocument.setType("Document");
            //get the caseTypeId and JID
            String caseTypeId = caseDetails.getCaseTypeId();
            String jurisdictionId = caseDetails.getJurisdiction();
            //retrieve the caseType and the documentCaseField
            final CaseType caseType = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
            final Optional<CaseField> documentCaseField = caseType.getCaseField(documentField);

            //get the users role
            Set<String> roles = getUserRoles(caseDetails.getId());

            //retrieve all ACL on the user roles
            Optional<AccessControlList> userACLOnCaseField = Optional.empty();
            for (String role : roles) {
                userACLOnCaseField = documentCaseField.get().getAccessControlLists().stream().filter(acl -> acl.getRole().equalsIgnoreCase(role)).findFirst();
            }
            if (userACLOnCaseField.isPresent()) {
                setDocumentPermissions(userACLOnCaseField.get(), caseDocument);
            }

            return caseDocument;

        } else {
            throw new CaseDocumentNotFoundException(caseDetails.getReferenceAsString());
        }
    }

    private Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
                .stream()
                .collect(Collectors.toSet()));
    }

    private String getDocumentCaseField(Map<String, JsonNode> caseData, String documentId) {
        for (Map.Entry<String, JsonNode> entry : caseData.entrySet()) {
            //if (entry.getValue().toString().contains(documentId)) {
            if (entry.getValue().getNodeType().toString().equals("OBJECT") && entry.getValue().toString().contains(documentId)) {
                String key = entry.getKey();
                System.out.println("Key found :" + key);
                return key;
            }
        }
        return null;
    }


    private void setDocumentPermissions(AccessControlList userACL, CaseDocument caseDocument) {
        final List<Permission> permissions = new ArrayList();

        if (userACL.isRead()) {
            permissions.add(Permission.READ);
        }

        if (userACL.isUpdate()) {
            permissions.add(Permission.UPDATE);
        }
        caseDocument.setPermissions(permissions);
    }

    private CaseDocumentMetadata prepareDocumentMetadata(CaseDetails caseDetails, CaseDocument caseDocument) {
//        CaseDocumentMetadata documentMetadata = new CaseDocumentMetadata();
//        documentMetadata.setCaseId(caseDetails.getReferenceAsString());
//        documentMetadata.setCaseTypeId(caseDetails.getCaseTypeId());
//        documentMetadata.setJurisdictionId(caseDetails.getJurisdiction());
//        documentMetadata.setDocument(caseDocument);
//        return documentMetadata;
        return CaseDocumentMetadata.builder()
            .caseId(caseDetails.getReferenceAsString())
            .caseTypeId(caseDetails.getCaseTypeId())
            .jurisdictionId(caseDetails.getJurisdiction())
            .document(caseDocument)
            .build();
    }
}
