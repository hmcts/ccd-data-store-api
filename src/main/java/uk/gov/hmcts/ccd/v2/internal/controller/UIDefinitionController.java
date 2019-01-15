package uk.gov.hmcts.ccd.v2.internal.controller;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

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
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedFindWorkbasketInputOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.FindWorkbasketInputOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UIWorkbasketInputsResource;

@RestController
@RequestMapping(path = "/internal")
public class UIDefinitionController {

    private final FindWorkbasketInputOperation findWorkbasketInputOperation;

    @Autowired
    public UIDefinitionController(@Qualifier(AuthorisedFindWorkbasketInputOperation.QUALIFIER) FindWorkbasketInputOperation findWorkbasketInputOperation) {
        this.findWorkbasketInputOperation = findWorkbasketInputOperation;
    }


    @GetMapping(
        path = "/case-types/{caseTypeId}/work-basket-inputs",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_WORKBASKET_INPUT_DETAILS
        }
    )
    @ApiOperation(
        value = "Retrieve workbasket input details for dynamic display",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = UIWorkbasketInputsResource.class
        ),
        @ApiResponse(
            code = 404,
            message = "Case type not found"
        )
    })
    public ResponseEntity<UIWorkbasketInputsResource> getWorkbasketInputsDetails(@PathVariable("caseTypeId") String caseTypeId) {

        WorkbasketInput[] workbasketInputs = findWorkbasketInputOperation.execute(caseTypeId, CAN_READ).toArray(new WorkbasketInput[0]);

        return ResponseEntity.ok(new UIWorkbasketInputsResource(workbasketInputs, caseTypeId));
    }
}
