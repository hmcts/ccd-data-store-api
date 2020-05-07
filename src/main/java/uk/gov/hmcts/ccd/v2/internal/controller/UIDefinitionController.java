package uk.gov.hmcts.ccd.v2.internal.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.*;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;

@RestController
@RequestMapping(path = "/internal")
public class UIDefinitionController {

    private final GetCriteriaOperation getCriteriaOperation;

    private final GetBannerOperation getBannerOperation;

    private final GetJurisdictionUiConfigOperation getJurisdictionUiConfigOperation;

    private final GetUserProfileOperation getUserProfileOperation;

    private final HashMap<String, Predicate<AccessControlList>> accessMap = new HashMap<>();

    @Autowired
    public UIDefinitionController(@Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER) GetCriteriaOperation getCriteriaOperation,
                                  @Qualifier(DefaultGetBannerOperation.QUALIFIER) GetBannerOperation getBannerOperation,
                                  @Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER) final GetUserProfileOperation getUserProfileOperation,
                                  @Qualifier(DefaultGetJurisdictionUiConfigOperation.QUALIFIER)
                                          GetJurisdictionUiConfigOperation getJurisdictionUiConfigOperation) {
        this.getCriteriaOperation = getCriteriaOperation;
        this.getBannerOperation = getBannerOperation;
        this.getJurisdictionUiConfigOperation = getJurisdictionUiConfigOperation;
        this.getUserProfileOperation = getUserProfileOperation;
        accessMap.put("create", CAN_CREATE);
        accessMap.put("update", CAN_UPDATE);
        accessMap.put("read", CAN_READ);
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
            response = WorkbasketInputsViewResource.class
        ),
        @ApiResponse(
            code = 404,
            message = "Case type not found"
        )
    })
    public ResponseEntity<WorkbasketInputsViewResource> getWorkbasketInputsDetails(@PathVariable("caseTypeId") String caseTypeId) {

        WorkbasketInput[] workbasketInputs = getCriteriaOperation.execute(caseTypeId, CAN_READ, WORKBASKET).toArray(new WorkbasketInput[0]);

        return ResponseEntity.ok(new WorkbasketInputsViewResource(workbasketInputs, caseTypeId));
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
            response = SearchInputsViewResource.class
        ),
        @ApiResponse(
            code = 404,
            message = "Case type not found"
        )
    })
    public ResponseEntity<SearchInputsViewResource> getSearchInputsDetails(@PathVariable("caseTypeId") String caseTypeId) {

        SearchInput[] searchInputs = getCriteriaOperation.execute(caseTypeId, CAN_READ, SEARCH).toArray(new SearchInput[0]);

        return ResponseEntity.ok(new SearchInputsViewResource(searchInputs, caseTypeId));
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
            response = BannerViewResource.class
        )
    })
    public ResponseEntity<BannerViewResource> getBanners(@RequestParam("ids") Optional<List<String>> idsOptional) {
        List<Banner> listOfBanners = idsOptional.isPresent()
            ? getBannerOperation.execute(idsOptional.get())
            : Lists.newArrayList();
        return ResponseEntity.ok(new BannerViewResource(listOfBanners));
    }

    @GetMapping(
        path = "/jurisdiction-ui-configs",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_JURISDICTION_CONFIGS
        }
    )
    @ApiOperation(
        value = "Get Jurisdiction UI config information for the jurisdictions",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = JurisdictionConfigViewResource.class
        )
    })
    public ResponseEntity<JurisdictionConfigViewResource> getJurisdictionUiConfigs(@RequestParam("ids") Optional<List<String>> idsOptional) {
        List<JurisdictionUiConfigDefinition> listOfConfigs = idsOptional.isPresent()
            ? getJurisdictionUiConfigOperation.execute(idsOptional.get())
            : Lists.newArrayList();
        return ResponseEntity.ok(new JurisdictionConfigViewResource(listOfConfigs));
    }

    @GetMapping(
        path = "/jurisdictions",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_JURISDICTIONS
        }
    )
    @ApiOperation(
        value = "Get Jurisdictions information for the access type passed",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = JurisdictionViewResource.class
        ),
        @ApiResponse(
            code = 404,
            message = "No jurisdictions found"
        ),
        @ApiResponse(
            code = 400,
            message = "Access can only be 'create', 'read' or 'update'"
        )
    })
    public ResponseEntity<JurisdictionViewResource> getJurisdictions(@RequestParam(value = "access") String access) {
        if (accessMap.get(access) == null) {
            throw new BadRequestException("Access can only be 'create', 'read' or 'update'");
        }
        JurisdictionDisplayProperties[] jurisdictions = getUserProfileOperation.execute(accessMap.get(access)).getJurisdictions();
        if (jurisdictions == null || jurisdictions.length == 0) {
            throw new ResourceNotFoundException("No jurisdictions found");
        }
        return ResponseEntity.ok(new JurisdictionViewResource(jurisdictions, access));
    }
}
