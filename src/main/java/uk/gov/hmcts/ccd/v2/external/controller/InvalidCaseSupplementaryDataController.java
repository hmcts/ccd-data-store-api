package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.InvalidSupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataRequest;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ApplicationParams applicationParams;


    @Autowired
    public InvalidCaseSupplementaryDataController(ApplicationParams applicationParams,
                                                  InvalidSupplementaryDataOperation invalidSupplementaryDataOperation,
                                                  @Qualifier("authorised") CaseAssignedUserRolesOperation
                                                      caseAssignedUserRolesOperation) {
        this.applicationParams = applicationParams;
        this.invalidSupplementaryDataOperation = invalidSupplementaryDataOperation;
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
    }

    @PostMapping(
        path = "/case-users/search-invalid-supplementary-data",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_DATA_VALIDATE
        }
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
        List<String> casesList = null;
        validateRequestParams(request);

        for (String caseType: applicationParams.getInvalidSupplementaryDataCaseTypes()) {
            casesList.addAll(invalidSupplementaryDataOperation.getInvalidSupplementaryDataCases(caseType, request.getDateFrom(), request.getDateTo(), request.getLimit()));

        }
        if (request.getSearchRas()) {
            List<CaseAssignedUserRole> caseAssignedUserRoles = this.caseAssignedUserRolesOperation
                .findCaseUserRoles(casesList.stream().map(Long::valueOf)
                .collect(Collectors.toCollection(ArrayList::new)), emptyList());

            return ResponseEntity.ok(new InvalidCaseSupplementaryDataResponse(casesList, caseAssignedUserRoles));
        } else {
            return  ResponseEntity.ok(new InvalidCaseSupplementaryDataResponse(casesList, emptyList()));
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
