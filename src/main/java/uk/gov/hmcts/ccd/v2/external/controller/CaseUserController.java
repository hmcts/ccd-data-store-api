package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

@RestController
@RequestMapping(path = "/cases/{caseReference}/users")
public class CaseUserController {

    private final UIDService caseReferenceService;
    private final UserAuthorisation userAuthorisation;
    private final CaseDetailsRepository caseRepository;
    private final CaseAccessOperation caseAccessOperation;

    public CaseUserController(UIDService caseReferenceService,
                              UserAuthorisation userAuthorisation,
                              @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseRepository,
                              CaseAccessOperation caseAccessOperation) {
        this.caseReferenceService = caseReferenceService;
        this.userAuthorisation = userAuthorisation;
        this.caseRepository = caseRepository;
        this.caseAccessOperation = caseAccessOperation;
    }

    @PutMapping(
        path = "/{userId}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        description = "Update a user's roles for a specific case. Grant access for added case roles and revoke access for "
            + "removed case roles."
    )
    @ApiResponse(
        responseCode = "204",
        description = "Access granted"
    )
    @ApiResponse(
        responseCode = "400",
        description = V2.Error.CASE_ID_INVALID
    )
    @ApiResponse(
        responseCode = "400",
        description = V2.Error.CASE_ROLE_REQUIRED
    )
    @ApiResponse(
        responseCode = "400",
        description = V2.Error.CASE_ROLE_INVALID
    )
    @ApiResponse(
        responseCode = "403",
        description = V2.Error.GRANT_FORBIDDEN
    )
    @ApiResponse(
        responseCode = "404",
        description = V2.Error.CASE_NOT_FOUND
    )
    @LogAudit(operationType = AuditOperationType.UPDATE_CASE_ACCESS, caseId = "#caseReference",
        targetIdamId = "#userId",
        targetCaseRoles = "#caseUser.caseRoles")
    public ResponseEntity<Void> putUser(
        @PathVariable("caseReference") String caseReference,
        @PathVariable("userId") String userId,
        @RequestBody CaseUser caseUser
    ) {
        if (!caseReferenceService.validateUID(caseReference)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }

        if (null == caseUser || null == caseUser.getCaseRoles()) {
            throw new BadRequestException(V2.Error.CASE_ROLE_REQUIRED);
        }

        final CaseDetails caseDetails = caseRepository.findByReference(caseReference)
                                                      .orElseThrow(() -> new CaseNotFoundException(caseReference));

        if (!accessUpdateAuthorised(caseDetails)) {
            throw new ForbiddenException();
        }

        caseUser.setUserId(userId);
        caseAccessOperation.updateUserAccess(caseDetails, caseUser);

        return ResponseEntity.noContent()
                             .build();
    }

    private Boolean accessUpdateAuthorised(CaseDetails caseDetails) {
        return userAuthorisation.hasJurisdictionRole(caseDetails.getJurisdiction());
    }

}
