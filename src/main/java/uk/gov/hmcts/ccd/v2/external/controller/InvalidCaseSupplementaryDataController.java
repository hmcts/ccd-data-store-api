package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.InvalidSupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataItem;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataRequest;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@RestController
@RequestMapping(path = "/")
@ConditionalOnProperty(value = "ccd.conditional-apis.case-search-invalid-supplementary-data.enabled",
    havingValue = "true")
public class InvalidCaseSupplementaryDataController {
    private final InvalidSupplementaryDataOperation invalidSupplementaryDataOperation;
    private final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Autowired
    public InvalidCaseSupplementaryDataController(InvalidSupplementaryDataOperation invalidSupplementaryDataOperation,
                                                  @Qualifier("default") CaseAssignedUserRolesOperation
                                                      caseAssignedUserRolesOperation) {
        this.invalidSupplementaryDataOperation = invalidSupplementaryDataOperation;
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
    }

    @PostMapping(
        path = "/case-users/search-invalid-supplementary-data",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = V2.MediaType.CASE_DATA_VALIDATE
    )
    @ApiOperation(
        value = "Get Cases that have HMCTSServiceId but no orgs_assigned_users in supplementary_data"
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Cases returned successfully",
            response = InvalidCaseSupplementaryDataResponse.class
        ),
        @ApiResponse(
            code = 400,
            message = "Invalid request parameters. "
        )
    })
    public ResponseEntity<InvalidCaseSupplementaryDataResponse> getInvalidSupplementaryData(
        @ApiParam(value = "Parameters to filter on", required = true)
        @RequestBody InvalidCaseSupplementaryDataRequest request) {
        validateRequestParams(request);

        List<InvalidCaseSupplementaryDataItem> invalidSupplementaryDataCases = invalidSupplementaryDataOperation
            .getInvalidSupplementaryDataCases(request.getDateFrom(), request.getDateTo(), request.getLimit());

        if (request.getSearchRas() != null && request.getSearchRas()) {
            List<Long> casesList = invalidSupplementaryDataCases.stream()
                .map(InvalidCaseSupplementaryDataItem::getCaseId).collect(Collectors.toList());

            List<CaseAssignedUserRole> caseAssignedUserRoles = this.caseAssignedUserRolesOperation
                .findCaseUserRoles(casesList, emptyList());

            invalidSupplementaryDataCases.forEach(e -> enhanceWithUserRoles(e, caseAssignedUserRoles));
        }

        return ResponseEntity.ok(InvalidCaseSupplementaryDataResponse.builder()
            .dataItems(invalidSupplementaryDataCases).build());
    }

    private void enhanceWithUserRoles(InvalidCaseSupplementaryDataItem invalidSupplementaryDataCase,
                                      List<CaseAssignedUserRole> caseAssignedUserRoles) {

        Long invalidCaseId = invalidSupplementaryDataCase.getCaseId();
        Optional<CaseAssignedUserRole> userRole = caseAssignedUserRoles.stream()
            .filter(e -> invalidCaseId.toString().equals(e.getCaseDataId()))
            .findFirst();

        if (userRole.isPresent()) {
            invalidSupplementaryDataCase.setUserId(userRole.get().getUserId());
            invalidSupplementaryDataCase.setCaseRole(userRole.get().getCaseRole());
        }
    }

    private void validateRequestParams(InvalidCaseSupplementaryDataRequest request) {
        if (request.getDateFrom() == null) {
            throw new BadRequestException("Invalid parameters: 'date_from' has to be defined");
        } else {
            Optional<LocalDateTime> dateTo = request.getDateTo();
            if (dateTo.isPresent()) {
                if (request.getDateFrom().isAfter(dateTo.get())) {
                    throw new BadRequestException("Invalid parameters: 'date_from' has to be before 'date_to'");
                }
            }
        }
    }
}
