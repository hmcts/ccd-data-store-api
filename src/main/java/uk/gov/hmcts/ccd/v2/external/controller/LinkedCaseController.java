package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

@RestController
@RequestMapping(path = "/getLinkedCase")
public class LinkedCaseController {
    private final GetCaseOperation getCaseOperation;
    private final CreateEventOperation createEventOperation;
    private final CreateCaseOperation createCaseOperation;
    private final UIDService caseReferenceService;
    private final GetEventsOperation getEventsOperation;
    private final SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;
    private final SupplementaryDataUpdateRequestValidator requestValidator;

    @Autowired
    public LinkedCaseController(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        @Qualifier("authorised") final CreateEventOperation createEventOperation,
        @Qualifier("authorised") final CreateCaseOperation createCaseOperation,
        UIDService caseReferenceService,
        @Qualifier("authorised") GetEventsOperation getEventsOperation,
        @Qualifier("authorised") SupplementaryDataUpdateOperation supplementaryDataUpdateOperation,
        SupplementaryDataUpdateRequestValidator requestValidator
    ) {
        this.getCaseOperation = getCaseOperation;
        this.createEventOperation = createEventOperation;
        this.createCaseOperation = createCaseOperation;
        this.caseReferenceService = caseReferenceService;
        this.getEventsOperation = getEventsOperation;
        this.supplementaryDataUpdateOperation = supplementaryDataUpdateOperation;
        this.requestValidator = requestValidator;
    }

    @GetMapping(
        path = "/{caseReference}",
        produces = {
            V2.MediaType.CASE
        }
    )
    @ApiOperation(
        value = "Retrieve a Linked Case",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseResource.class
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
    /*TODO @LogAudit(operationType = LINKED_CASE_ACCESSED, caseId = "#caseId",
        jurisdiction = "#result.body.jurisdiction", caseType = "#result.body.caseType")*/
    public ResponseEntity<Void> getLinkedCase(@PathVariable("caseReference") String caseReference,
                                              @RequestParam(name = "startRecordNumber",
                                                  defaultValue = "1", required = false) String startRecordNumber,
                                              @RequestParam(name = "maxReturnRecordCount",
                                                  required = false) String maxReturnRecordCount) {
        //Validate Case Reference is valid
        validateCaseReference(caseReference);

        //Validate Case exists
        final CaseDetails caseDetails = getCaseDetails(caseReference);

        //Validate parameters are numeric
        validateIsParamNum(startRecordNumber);
        validateIsParamNum(maxReturnRecordCount);

        return ResponseEntity.noContent().build();  // TODO: for now
    }

    private void validateIsParamNum(String number) {
        if (number != null) {
            try {
                Long.parseLong(number);
            } catch (NumberFormatException nfe) {
                throw new BadRequestException(V2.Error.PARAM_NOT_NUM);
            }
        }
    }

    private void validateCaseReference(final String caseReference) {
        if (!caseReferenceService.validateUID(caseReference)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }
    }

    private CaseDetails getCaseDetails(final String caseReference) {
        return getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}

