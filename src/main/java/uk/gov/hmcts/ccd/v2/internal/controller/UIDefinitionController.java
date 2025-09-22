package uk.gov.hmcts.ccd.v2.internal.controller;

import com.google.common.collect.Lists;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetJurisdictionUiConfigOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetJurisdictionUiConfigOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.BannerViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.JurisdictionConfigViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.JurisdictionViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.SearchInputsViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.WorkbasketInputsViewResource;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@RestController
@RequestMapping(path = "/internal")
public class UIDefinitionController {

    private final GetCriteriaOperation getCriteriaOperation;

    private final GetBannerOperation getBannerOperation;

    private final GetJurisdictionUiConfigOperation getJurisdictionUiConfigOperation;

    private final GetUserProfileOperation getUserProfileOperation;

    private final HashMap<String, Predicate<AccessControlList>> accessMap = new HashMap<>();

    @Autowired
    public UIDefinitionController(@Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER)
                                          GetCriteriaOperation getCriteriaOperation,
                                  @Qualifier(DefaultGetBannerOperation.QUALIFIER) GetBannerOperation getBannerOperation,
                                  @Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER)
                                      final GetUserProfileOperation getUserProfileOperation,
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
    @Operation(
        summary = "Retrieve workbasket input details for dynamic display",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = WorkbasketInputsViewResource.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Case type not found"
    )
    public ResponseEntity<WorkbasketInputsViewResource> getWorkbasketInputsDetails(@PathVariable("caseTypeId")
                                                                                           String caseTypeId) {

        WorkbasketInput[] workbasketInputs =
            getCriteriaOperation.execute(caseTypeId, CAN_READ, WORKBASKET).toArray(new WorkbasketInput[0]);

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
    @Operation(
        summary = "Retrieve search input details for dynamic display",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = SearchInputsViewResource.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Case type not found"
    )
    public ResponseEntity<SearchInputsViewResource> getSearchInputsDetails(@PathVariable("caseTypeId")
                                                                                   String caseTypeId) {

        SearchInput[] searchInputs =
            getCriteriaOperation.execute(caseTypeId, CAN_READ, SEARCH).toArray(new SearchInput[0]);

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
    @Operation(
        summary = "Get Banner information for the jurisdictions",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = BannerViewResource.class))
    )
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
    @Operation(
        summary = "Get Jurisdiction UI config information for the jurisdictions",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = JurisdictionConfigViewResource.class))
    )
    public ResponseEntity<JurisdictionConfigViewResource> getJurisdictionUiConfigs(@RequestParam("ids")
                                                                                   Optional<List<String>> idsOptional) {
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
    @Operation(
        summary = "Get Jurisdictions information for the access type passed",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = JurisdictionViewResource.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "No jurisdictions found"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Access can only be 'create', 'read' or 'update'"
    )
    public ResponseEntity<JurisdictionViewResource> getJurisdictions(@RequestParam(value = "access") String access) {
        if (accessMap.get(access) == null) {
            throw new BadRequestException("Access can only be 'create', 'read' or 'update'");
        }
        JurisdictionDisplayProperties[] jurisdictions =
            getUserProfileOperation.execute(accessMap.get(access)).getJurisdictions();
        if (jurisdictions == null || jurisdictions.length == 0) {
            throw new ResourceNotFoundException("No jurisdictions found");
        }
        return ResponseEntity.ok(new JurisdictionViewResource(jurisdictions, access));
    }
}
