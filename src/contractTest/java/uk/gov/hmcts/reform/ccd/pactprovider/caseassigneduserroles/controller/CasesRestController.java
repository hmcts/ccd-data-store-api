package uk.gov.hmcts.reform.ccd.pactprovider.caseassigneduserroles.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.ccd.ApplicationParams;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

@RestController
public class CasesRestController {

    private final Pattern caseRolePattern = Pattern.compile("^.+$");

    private final ApplicationParams applicationParams;
    private final UIDService caseReferenceService;
    private final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;
    private final SecurityUtils securityUtils;

    public CasesRestController(final ApplicationParams applicationParams,
                               final UIDService caseReferenceService,
                               final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation,
                               final SecurityUtils securityUtils) {
        this.applicationParams = applicationParams;
        this.caseReferenceService = caseReferenceService;
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
        this.securityUtils = securityUtils;
    }

    /*
     * Handle Pact State GET "A User Role exists for a Case".
     */
    @Operation(description = "A User Role exists for a Case",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @GetMapping(path = "/case-users", produces = APPLICATION_JSON_VALUE)
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


    /*
     * Handle Pact State DELETE "A User Role exists for a Case".
     */
    @Operation(description = "A User Role exists for a Case",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @DeleteMapping(path = "/case-users", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseAssignedUserRolesResponse> removeCaseUserRoles(
        @ApiParam(value = "Valid Service-to-Service JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken,
        @ApiParam(value = "List of Case-User-Role assignments to add", required = true)
        @RequestBody CaseAssignedUserRolesRequest caseAssignedUserRolesRequest
    ) {
        validateRequest(clientS2SToken, caseAssignedUserRolesRequest);
        this.caseAssignedUserRolesOperation.removeCaseUserRoles(caseAssignedUserRolesRequest
            .getCaseAssignedUserRoles());
        return ResponseEntity.status(HttpStatus.OK).body(new CaseAssignedUserRolesResponse("REMOVED"));
    }


    private void validateRequest(String clientS2SToken, CaseAssignedUserRolesRequest request) {
        String clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        if (applicationParams.getAuthorisedServicesForCaseUserRoles().contains(clientServiceName)) {
            validateRequestParams(request);
        } else {
            throw new CaseRoleAccessException(V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION);
        }
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

    private static Stream<CaseAssignedUserRoleWithOrganisation> caseAssignedUserRolesToStream(
        CaseAssignedUserRolesRequest addCaseAssignedUserRolesRequest) {
        return addCaseAssignedUserRolesRequest != null
            ? Optional.ofNullable(addCaseAssignedUserRolesRequest.getCaseAssignedUserRoles())
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            : Stream.empty();
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
}
