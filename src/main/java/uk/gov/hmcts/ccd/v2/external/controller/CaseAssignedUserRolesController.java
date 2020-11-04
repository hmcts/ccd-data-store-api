package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseRoleAccessException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseAssignedUserRolesRequest;
import uk.gov.hmcts.ccd.v2.external.domain.CaseAssignedUserRolesResponse;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.ADD_CASE_ASSIGNED_USER_ROLES;
import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.GET_CASE_ASSIGNED_USER_ROLES;
import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.REMOVE_CASE_ASSIGNED_USER_ROLES;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

@RestController
@RequestMapping(path = "/")
@ConditionalOnProperty(value = "ccd.conditional-apis.case-assigned-users-and-roles.enabled", havingValue = "true")
public class CaseAssignedUserRolesController {

    public static final String ADD_SUCCESS_MESSAGE = "Case-User-Role assignments created successfully";
    public static final String REMOVE_SUCCESS_MESSAGE = "Case-User-Role assignments removed successfully";

    final Pattern caseRolePattern = Pattern.compile("^\\[.+]$");

    private final ApplicationParams applicationParams;
    private final UIDService caseReferenceService;
    private final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseAssignedUserRolesController(ApplicationParams applicationParams,
                                           UIDService caseReferenceService,
                                           @Qualifier("authorised") CaseAssignedUserRolesOperation
                                                   caseAssignedUserRolesOperation,
                                           SecurityUtils securityUtils) {
        this.applicationParams = applicationParams;
        this.caseReferenceService = caseReferenceService;
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
        this.securityUtils = securityUtils;
    }

    @PostMapping(
        path = "/case-users"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Add Case-Assigned Users and Roles"
    )
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = ADD_SUCCESS_MESSAGE,
            response = CaseAssignedUserRolesResponse.class
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:\n"
                + "1. " + V2.Error.EMPTY_CASE_USER_ROLE_LIST + ", \n"
                + "2. " + V2.Error.CASE_ID_INVALID + ": has to be a valid 16-digit Luhn number, \n"
                + "3. " + V2.Error.USER_ID_INVALID + ": has to be a string of length > 0, \n"
                + "4. " + V2.Error.CASE_ROLE_FORMAT_INVALID + ": has to be a none-empty string in square brackets, \n"
                + "5. " + V2.Error.ORGANISATION_ID_INVALID + ": has to be a non-empty string, when present."
        ),
        @ApiResponse(
            code = 401,
            message = V2.Error.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = "One of the following reasons:\n"
                + "1. " + V2.Error.UNAUTHORISED_S2S_SERVICE + "\n"
                + "2. " + V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION + "."
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
        )
    })
    @LogAudit(
        operationType = ADD_CASE_ASSIGNED_USER_ROLES,
        caseId = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
            + ".buildCaseIds(#caseAssignedUserRolesRequest)",
        targetIdamId = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
            + ".buildUserIds(#caseAssignedUserRolesRequest)",
        targetCaseRoles = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
            + ".buildCaseRoles(#caseAssignedUserRolesRequest)"
    )
    public ResponseEntity<CaseAssignedUserRolesResponse> addCaseUserRoles(
        @ApiParam(value = "Valid Service-to-Service JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken,
        @ApiParam(value = "List of Case-User-Role assignments to add", required = true)
        @RequestBody CaseAssignedUserRolesRequest caseAssignedUserRolesRequest
    ) {
        validateRequest(clientS2SToken, caseAssignedUserRolesRequest);
        this.caseAssignedUserRolesOperation.addCaseUserRoles(caseAssignedUserRolesRequest.getCaseAssignedUserRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CaseAssignedUserRolesResponse(ADD_SUCCESS_MESSAGE));
    }

    @DeleteMapping(
            path = "/case-users"
    )
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
            value = "Remove Case-Assigned Users and Roles"
    )
    @ApiResponses({
            @ApiResponse(
                    code = 200,
                    message = REMOVE_SUCCESS_MESSAGE,
                    response = CaseAssignedUserRolesResponse.class
            ),
            @ApiResponse(
                    code = 400,
                    message = "One or more of the following reasons:\n"
                            + "1. " + V2.Error.EMPTY_CASE_USER_ROLE_LIST + ", \n"
                            + "2. " + V2.Error.CASE_ID_INVALID + ": has to be a valid 16-digit Luhn number, \n"
                            + "3. " + V2.Error.USER_ID_INVALID + ": has to be a string of length > 0, \n"
                            + "4. " + V2.Error.CASE_ROLE_FORMAT_INVALID + ": has to be a none-empty string in square "
                                + "brackets, \n"
                            + "5. " + V2.Error.ORGANISATION_ID_INVALID + ": has to be a non-empty string, when present."
            ),
            @ApiResponse(
                    code = 401,
                    message = "Authentication failure due to invalid / expired tokens (IDAM / S2S)."
            ),
            @ApiResponse(
                    code = 403,
                    message = "One of the following reasons:\n"
                            + "1. Unauthorised S2S service \n"
                            + "2. " + V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION + "."
            ),
            @ApiResponse(
                    code = 404,
                    message = V2.Error.CASE_NOT_FOUND
            )
    })
    @LogAudit(
            operationType = REMOVE_CASE_ASSIGNED_USER_ROLES,
            caseId = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
                + ".buildCaseIds(#caseAssignedUserRolesRequest)",
            targetIdamId = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
                + ".buildUserIds(#caseAssignedUserRolesRequest)",
            targetCaseRoles = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
                + ".buildCaseRoles(#caseAssignedUserRolesRequest)"
    )
    public ResponseEntity<CaseAssignedUserRolesResponse> removeCaseUserRoles(
            @ApiParam(value = "Valid Service-to-Service JWT token for an approved micro-service", required = true)
            @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken,
            @ApiParam(value = "List of Case-User-Role assignments to add", required = true)
            @RequestBody CaseAssignedUserRolesRequest caseAssignedUserRolesRequest
    ) {
        validateRequest(clientS2SToken, caseAssignedUserRolesRequest);
        this.caseAssignedUserRolesOperation.removeCaseUserRoles(caseAssignedUserRolesRequest
            .getCaseAssignedUserRoles());
        return ResponseEntity.status(HttpStatus.OK).body(new CaseAssignedUserRolesResponse(REMOVE_SUCCESS_MESSAGE));
    }

    @GetMapping(
        path = "/case-users"
    )
    @ApiOperation(
        value = "Get Case-Assigned Users and Roles"
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
    @LogAudit(
        operationType = GET_CASE_ASSIGNED_USER_ROLES,
        caseId = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
            + ".buildOptionalIds(#caseIds)",
        targetIdamId = "T(uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController)"
            + ".buildOptionalIds(#optionalUserIds)"
    )
    public ResponseEntity<CaseAssignedUserRolesResource> getCaseUserRoles(@RequestParam("case_ids")
                                                                                  List<String> caseIds,
                                                                          @RequestParam(value = "user_ids",
                                                                              required = false)
                                                                              Optional<List<String>> optionalUserIds) {
        List<String> userIds = optionalUserIds.orElseGet(Lists::newArrayList);
        validateRequestParams(caseIds, userIds);
        List<CaseAssignedUserRole> caseAssignedUserRoles = this.caseAssignedUserRolesOperation.findCaseUserRoles(caseIds
            .stream()
            .map(Long::valueOf)
            .collect(Collectors.toCollection(ArrayList::new)), userIds);
        return ResponseEntity.ok(new CaseAssignedUserRolesResource(caseAssignedUserRoles));
    }

    private void validateRequestParams(List<String> caseIds, List<String> userIds) {
        List<String> errorMessages =
            Lists.newArrayList("Invalid data provided for the following inputs to the request:");
        if (caseIds == null || caseIds.isEmpty()) {
            errorMessages.add(V2.Error.EMPTY_CASE_ID_LIST);
        } else {
            caseIds.forEach(caseId -> {
                if (!caseReferenceService.validateUID(caseId)) {
                    errorMessages.add(V2.Error.CASE_ID_INVALID);
                }
            });
        }

        userIds.forEach(userId -> {
            if (StringUtils.isAllBlank(userId)) {
                errorMessages.add(V2.Error.USER_ID_INVALID);
            }
        });

        if (errorMessages.size() > 1) {
            String message = String.join("\n", errorMessages);
            throw new BadRequestException(message);
        }
    }

    private void validateRequestParams(CaseAssignedUserRolesRequest addCaseAssignedUserRolesRequest) {

        List<String> errorMessages =
            Lists.newArrayList("Invalid data provided for the following inputs to the request:");

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles =
            caseAssignedUserRolesToStream(addCaseAssignedUserRolesRequest).collect(Collectors.toList());

        /// case-users: must be none empty
        if (caseUserRoles.isEmpty()) {
            errorMessages.add(V2.Error.EMPTY_CASE_USER_ROLE_LIST);
        } else {
            caseUserRoles.forEach(caseRole -> validateCaseAssignedUserRoleRequest(caseRole, errorMessages));
        }

        if (errorMessages.size() > 1) {
            String message = String.join("\n", errorMessages);
            throw new BadRequestException(message);
        }
    }

    private void validateCaseAssignedUserRoleRequest(CaseAssignedUserRoleWithOrganisation caseRole,
                                                     List<String> errorMessages) {
        // case_id: has to be a valid 16-digit Luhn number
        if (!caseReferenceService.validateUID(caseRole.getCaseDataId())) {
            errorMessages.add(V2.Error.CASE_ID_INVALID);
        }
        // user_id: has to be a string of length > 0
        if (StringUtils.isAllBlank(caseRole.getUserId())) {
            errorMessages.add(V2.Error.USER_ID_INVALID);
        }
        // case_role: has to be a none-empty string in square brackets
        if (caseRole.getCaseRole() == null || !caseRolePattern.matcher(caseRole.getCaseRole()).matches()) {
            errorMessages.add(V2.Error.CASE_ROLE_FORMAT_INVALID);
        }
        // organisation_id: has to be a non-empty string, when present
        if (caseRole.getOrganisationId() != null && caseRole.getOrganisationId().isEmpty()) {
            errorMessages.add(V2.Error.ORGANISATION_ID_INVALID);
        }
    }

    public static String buildCaseIds(CaseAssignedUserRolesRequest addCaseAssignedUserRolesRequest) {
        return caseAssignedUserRolesToStream(addCaseAssignedUserRolesRequest).limit(MAX_CASE_IDS_LIST)
            .map(CaseAssignedUserRole::getCaseDataId)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    public static String buildCaseRoles(CaseAssignedUserRolesRequest addCaseAssignedUserRolesRequest) {
        // NB: match Case ID list size and separator configuration
        return caseAssignedUserRolesToStream(addCaseAssignedUserRolesRequest).limit(MAX_CASE_IDS_LIST)
            .map(CaseAssignedUserRole::getCaseRole)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    public static String buildUserIds(CaseAssignedUserRolesRequest addCaseAssignedUserRolesRequest) {
        // NB: match Case ID list size and separator configuration
        return caseAssignedUserRolesToStream(addCaseAssignedUserRolesRequest).limit(MAX_CASE_IDS_LIST)
            .map(CaseAssignedUserRole::getUserId)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    public static String buildOptionalIds(Optional<List<String>> optionalIds) {
        List<String> userIds = optionalIds.orElseGet(Lists::newArrayList);
        // NB: match Case ID list size and separator configuration
        return userIds.stream().limit(MAX_CASE_IDS_LIST)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    private static Stream<CaseAssignedUserRoleWithOrganisation> caseAssignedUserRolesToStream(
        CaseAssignedUserRolesRequest addCaseAssignedUserRolesRequest) {
        return addCaseAssignedUserRolesRequest != null
            ? Optional.ofNullable(addCaseAssignedUserRolesRequest.getCaseAssignedUserRoles())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
            : Stream.empty();
    }

    private void validateRequest(String clientS2SToken, CaseAssignedUserRolesRequest request) {

        String clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        if (applicationParams.getAuthorisedServicesForCaseUserRoles().contains(clientServiceName)) {
            validateRequestParams(request);
        } else {
            throw new CaseRoleAccessException(V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION);
        }
    }

}
