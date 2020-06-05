package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

@RestController
@RequestMapping(path = "/")
public class CaseAssignedUserRolesController {

    private final UIDService caseReferenceService;
    private final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Autowired
    public CaseAssignedUserRolesController(UIDService caseReferenceService,
                                           @Qualifier("authorised") CaseAssignedUserRolesOperation caseAssignedUserRolesOperation) {
        this.caseReferenceService = caseReferenceService;
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
    }

    @GetMapping(
        path = "/case-users"
    )
    @ApiOperation(
        value = "Retrieve Case user roles by Case ID and User ID"
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Case-User-Role assignments returned successfully",
            response = CaseAssignedUserRolesResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.EMPTY_CASE_ID_LIST
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.USER_ID_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = V2.Error.OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED
        )
    })
    public ResponseEntity<CaseAssignedUserRolesResource> getCaseUserRoles(@RequestParam("case_ids") List<String> caseIds,
                                                                          @RequestParam(value = "user_ids", required = false) List<String> userIds) {
        validateRequestParams(caseIds, userIds);
        List<CaseAssignedUserRole> caseAssignedUserRoles = this.caseAssignedUserRolesOperation.findCaseUserRoles(caseIds
            .stream()
            .map(caseId -> Long.valueOf(caseId))
            .collect(Collectors.toCollection(ArrayList::new)), userIds);
        return ResponseEntity.ok(new CaseAssignedUserRolesResource(caseAssignedUserRoles));
    }

    private void validateRequestParams(List<String> caseIds, List<String> userIds) {
        if (caseIds.isEmpty()) {
            throw new BadRequestException(V2.Error.EMPTY_CASE_ID_LIST);
        }

        caseIds.forEach(caseId -> {
            if (!caseReferenceService.validateUID(caseId)) {
                throw new BadRequestException(V2.Error.CASE_ID_INVALID);
            }
        });

        userIds.forEach(userId -> {
            if (StringUtils.isAllBlank(userId)) {
                throw new BadRequestException(V2.Error.USER_ID_INVALID);
            }
        });
    }
}
