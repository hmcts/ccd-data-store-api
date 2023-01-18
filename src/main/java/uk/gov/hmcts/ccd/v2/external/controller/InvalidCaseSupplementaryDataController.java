package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.InvalidSupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataRequest;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/")
@ConditionalOnProperty(value = "ccd.conditional-apis.case-search-invalid-supplementary-data.enabled",
    havingValue = "true")
public class InvalidCaseSupplementaryDataController {
    private final InvalidSupplementaryDataOperation invalidSupplementaryDataOperation;

    @Autowired
    public InvalidCaseSupplementaryDataController(InvalidSupplementaryDataOperation invalidSupplementaryDataOperation) {
        this.invalidSupplementaryDataOperation = invalidSupplementaryDataOperation;
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
            response = CaseAssignedUserRolesResource.class
        ),
        @ApiResponse(
            code = 400,
            message = "Invalid request parameters. "
        )
    })
    public @ResponseBody List<String> getInvalidSupplementaryData(@ApiParam(value = "Parameters to filter on",
        required = true) @RequestBody InvalidCaseSupplementaryDataRequest request) {
        validateRequestParams(request);
        return invalidSupplementaryDataOperation.getInvalidSupplementaryDataCases(request.getDateFrom(),
            request.getDateTo(), request.getLimit());
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
