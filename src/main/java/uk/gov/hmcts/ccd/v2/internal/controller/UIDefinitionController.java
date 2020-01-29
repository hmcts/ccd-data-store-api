package uk.gov.hmcts.ccd.v2.internal.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfig;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetJurisdictionUiConfigOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UIBannerResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UIJurisdictionResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UIJurisdictionConfigResource;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetJurisdictionUiConfigOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.UISearchInputsResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UIWorkbasketInputsResource;

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
    public UIDefinitionController(@Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER) GetCriteriaOperation getCriteriaOperation,
                                  @Qualifier(DefaultGetBannerOperation.QUALIFIER) GetBannerOperation getBannerOperation,
                                  @Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER) final GetUserProfileOperation getUserProfileOperation,
                                  @Qualifier(DefaultGetJurisdictionUiConfigOperation.QUALIFIER) GetJurisdictionUiConfigOperation getJurisdictionUiConfigOperation) {
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
            response = UIJurisdictionConfigResource.class
        )
    })
    public ResponseEntity<UIJurisdictionConfigResource> getJurisdictionUiConfigs(@RequestParam("ids") Optional<List<String>> idsOptional) {
        List<JurisdictionUiConfig> listOfConfigs = idsOptional.isPresent()
                                        ? getJurisdictionUiConfigOperation.execute(idsOptional.get())
                                        : Lists.newArrayList();
        return ResponseEntity.ok(new UIJurisdictionConfigResource(listOfConfigs));
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
            response = UIJurisdictionResource.class
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
    public ResponseEntity<UIJurisdictionResource> getJurisdictions(@RequestParam(value = "access") String access) {
        if (accessMap.get(access) == null) {
            throw new BadRequestException("Access can only be 'create', 'read' or 'update'");
        }
        JurisdictionDisplayProperties[] jurisdictions =  getUserProfileOperation.execute(accessMap.get(access)).getJurisdictions();
        if (jurisdictions == null || jurisdictions.length == 0) {
            throw new ResourceNotFoundException("No jurisdictions found");
        }
        return ResponseEntity.ok(new UIJurisdictionResource(jurisdictions, access));
    }
}
