package uk.gov.hmcts.ccd.v2.internal.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UIBannerResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UISearchInputsResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UIWorkbasketInputsResource;

import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@RestController
@RequestMapping(path = "/internal")
public class UIDefinitionController {

    private final GetCriteriaOperation getCriteriaOperation;

    private final GetBannerOperation getBannerOperation;

    @Autowired
    public UIDefinitionController(@Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER) GetCriteriaOperation getCriteriaOperation,
                                  @Qualifier(DefaultGetBannerOperation.QUALIFIER) GetBannerOperation getBannerOperation) {
        this.getCriteriaOperation = getCriteriaOperation;
        this.getBannerOperation = getBannerOperation;
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

        WorkbasketInput[] workbasketInputs = getCriteriaOperation.execute(caseTypeId, CAN_READ, WORKBASKET).toArray(new WorkbasketInput[0]);

        return ResponseEntity.ok(new UIWorkbasketInputsResource(workbasketInputs, caseTypeId));
    }

    @GetMapping(
        path = "/case-types/{caseTypeId}/search-inputs",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_SEARCH_INPUT_DETAILS
        }
    )
    @ApiOperation(
        value = "Retrieve search input details for dynamic display",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = UISearchInputsResource.class
        ),
        @ApiResponse(
            code = 404,
            message = "Case type not found"
        )
    })
    public ResponseEntity<UISearchInputsResource> getSearchInputsDetails(@PathVariable("caseTypeId") String caseTypeId) {

        SearchInput[] searchInputs = getCriteriaOperation.execute(caseTypeId, CAN_READ, SEARCH).toArray(new SearchInput[0]);

        return ResponseEntity.ok(new UISearchInputsResource(searchInputs, caseTypeId));
    }

    @GetMapping(
        path = "/banners",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_BANNERS
        }
    )
    @ApiOperation(
        value = "Get Banner information for the jurisdictions",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = UIBannerResource.class
        )
    })
    public ResponseEntity<UIBannerResource> getBanners(@RequestParam("ids") Optional<List<String>> idsOptional) {
        List<Banner> listOfBanners = idsOptional.isPresent()
                                        ? getBannerOperation.execute(idsOptional.get())
                                        : Lists.newArrayList();
        return ResponseEntity.ok(new UIBannerResource(listOfBanners));
    }
}
