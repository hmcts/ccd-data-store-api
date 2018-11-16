package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.search.CreatorSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.PaginatedSearchMetaDataOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.domain.service.stdapi.DocumentsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.PAGE_PARAM;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.SORT_PARAM;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Standard case API")
public class CaseDetailsEndpoint {
    private final GetCaseOperation getCaseOperation;
    private final CreateCaseOperation createCaseOperation;
    private final CreateEventOperation createEventOperation;
    private final StartEventOperation startEventOperation;
    private final DocumentsOperation documentsOperation;
    private final SearchOperation searchOperation;
    private final PaginatedSearchMetaDataOperation paginatedSearchMetaDataOperation;
    private final AppInsights appInsights;
    private final FieldMapSanitizeOperation fieldMapSanitizeOperation;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final MidEventCallback midEventCallback;

    @Autowired
    public CaseDetailsEndpoint(@Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                               @Qualifier("authorised") final CreateCaseOperation createCaseOperation,
                               @Qualifier("authorised") final CreateEventOperation createEventOperation,
                               @Qualifier("authorised") final StartEventOperation startEventOperation,
                               @Qualifier(CreatorSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                               final FieldMapSanitizeOperation fieldMapSanitizeOperation,
                               final ValidateCaseFieldsOperation validateCaseFieldsOperation,
                               final DocumentsOperation documentsOperation,
                               final PaginatedSearchMetaDataOperation paginatedSearchMetaDataOperation,
                               final MidEventCallback midEventCallback,
                               final AppInsights appinsights) {
        this.getCaseOperation = getCaseOperation;
        this.createCaseOperation = createCaseOperation;
        this.createEventOperation = createEventOperation;
        this.startEventOperation = startEventOperation;
        this.searchOperation = searchOperation;
        this.fieldMapSanitizeOperation = fieldMapSanitizeOperation;
        this.documentsOperation = documentsOperation;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.paginatedSearchMetaDataOperation = paginatedSearchMetaDataOperation;
        this.midEventCallback = midEventCallback;
        this.appInsights = appinsights;
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}")
    @ApiOperation(value = "Get case", notes = "Retrieve an existing case with its state and data")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Case found for the given ID"),
        @ApiResponse(code = 400, message = "Invalid case ID"),
        @ApiResponse(code = 404, message = "No case found for the given ID")
    })
    public CaseDetails findCaseDetailsForCaseworker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId) {

        final Instant start = Instant.now();
        final CaseDetails caseDetails = getCaseOperation.execute(jurisdictionId, caseTypeId, caseId)
                            .orElseThrow(() -> new CaseNotFoundException(jurisdictionId, caseTypeId, caseId));
        final Duration duration = Duration.between(start, Instant.now());
        appInsights.trackRequest("findCaseDetailsForCaseworker", duration.toMillis(), true);
        return caseDetails;
    }

    @Transactional
    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}")
    @ApiOperation(value = "Get case", notes = "Retrieve an existing case with its state and data")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Case found for the given ID"),
        @ApiResponse(code = 400, message = "Invalid case ID"),
        @ApiResponse(code = 404, message = "No case found for the given ID")
    })
    public CaseDetails findCaseDetailsForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId) {

        return getCaseOperation.execute(jurisdictionId, caseTypeId, caseId)
                               .orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}/token")
    @ApiOperation(value = "Start event creation as Case worker", notes = "Start the event creation process for an existing case. Triggers `AboutToStart` callback.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event creation process started"),
        @ApiResponse(code = 404, message = "No case found for the given ID"),
        @ApiResponse(code = 422, message = "Process could not be started")
    })
    public StartEventTrigger startEventForCaseworker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @ApiParam(value = "Event ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @ApiParam(value = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCase(uid, jurisdictionId, caseTypeId, caseId, eventTriggerId, ignoreWarning);
    }

    @Transactional
    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}/token")
    @ApiOperation(value = "Start event creation as Citizen", notes = "Start the event creation process for an existing case. Triggers `AboutToStart` callback.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event creation process started"),
        @ApiResponse(code = 404, message = "No case found for the given ID"),
        @ApiResponse(code = 422, message = "Process could not be started")
    })
    public StartEventTrigger startEventForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @ApiParam(value = "Event ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @ApiParam(value = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCase(uid, jurisdictionId, caseTypeId, caseId, eventTriggerId, ignoreWarning);
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-triggers/{etid}/token")
    @ApiOperation(value = "Start case creation as Case worker", notes = "Start the case creation process for a new case. Triggers `AboutToStart` callback.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Case creation process started"),
        @ApiResponse(code = 422, message = "Process could not be started")
    })
    public StartEventTrigger startCaseForCaseworker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Event ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @ApiParam(value = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCaseType(uid, jurisdictionId, caseTypeId, eventTriggerId, ignoreWarning);
    }

    @Transactional
    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-triggers/{etid}/token")
    @ApiOperation(value = "Start case creation as Citizen", notes = "Start the case creation process for a new case. Triggers `AboutToStart` callback.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Case creation process started"),
        @ApiResponse(code = 422, message = "Process could not be started")
    })
    public StartEventTrigger startCaseForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Event ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @ApiParam(value = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCaseType(uid, jurisdictionId, caseTypeId, eventTriggerId, ignoreWarning);
    }

    @PostMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Submit case creation as Case worker",
        notes = "Complete the case creation process. This requires a valid event token to be provided, as generated by `startCaseForCaseworker`."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Case created"),
        @ApiResponse(code = 422, message = "Case submission failed"),
        @ApiResponse(code = 409, message = "Case reference not unique")
    })
    public CaseDetails saveCaseDetailsForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Should `AboutToSubmit` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning,
        @RequestBody final CaseDataContent content) {

        return createCaseOperation.createCaseDetails(uid, jurisdictionId, caseTypeId, content, ignoreWarning);
    }

    @PostMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Submit case creation as Citizen",
        notes = "Complete the case creation process. This requires a valid event token to be provided, as generated by `startCaseForCitizen`."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Case created"),
        @ApiResponse(code = 422, message = "Case submission failed"),
        @ApiResponse(code = 409, message = "Case reference not unique")
    })
    public CaseDetails saveCaseDetailsForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Should `AboutToSubmit` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning,
        @RequestBody final CaseDataContent content) {

        return createCaseOperation.createCaseDetails(uid, jurisdictionId, caseTypeId, content, ignoreWarning);
    }

    @PostMapping(value = {"/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/validate",
        "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/validate"})
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Validate a set of fields as Case worker",
        notes = "Validate the case data entered during the case/event creation process."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Validation OK"),
        @ApiResponse(code = 422, message = "Field validation failed"),
        @ApiResponse(code = 409, message = "Case reference not unique")
    })
    public JsonNode validateCaseDetails(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Page ID")
        @RequestParam(required = false) final String pageId,
        @RequestBody final CaseDataContent content) {

        Map<String, JsonNode> data = validateCaseFieldsOperation.validateCaseDetails(jurisdictionId,
            caseTypeId,
            content.getEvent(),
            content.getData());

        return midEventCallback.invoke(jurisdictionId,
            caseTypeId,
            content.getEvent(),
            data,
            pageId,
            content.getIgnoreWarning());
    }

    @Transactional
    @PostMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Submit event creation as Case worker",
        notes = "Complete the event creation process. This requires a valid event token to be provided, as generated by `startEventForCaseworker`."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Case event created"),
        @ApiResponse(code = 409, message = "Case altered outside of transaction"),
        @ApiResponse(code = 422, message = "Event submission failed")
    })
    public CaseDetails createCaseEventForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @RequestBody final CaseDataContent content) {
        return createEventOperation.createCaseEvent(uid, jurisdictionId, caseTypeId, caseId, content.getEvent(), content.getData(), content.getToken(), content.getIgnoreWarning());
    }

    @Transactional
    @PostMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Submit event creation as Citizen",
        notes = "Complete the event creation process. This requires a valid event token to be provided, as generated by `startEventForCitizen`."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Case event created"),
        @ApiResponse(code = 409, message = "Case altered outside of transaction"),
        @ApiResponse(code = 422, message = "Event submission failed")
    })
    public CaseDetails createCaseEventForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @RequestBody final CaseDataContent content) {
        return createEventOperation.createCaseEvent(uid, jurisdictionId, caseTypeId, caseId, content.getEvent(), content.getData(), content.getToken(), content.getIgnoreWarning());
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/documents")
    @ApiOperation(value = "Get a list of printable documents for the given case type ")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Documents list for the given case type and id")
    })
    public List<Document> getDocumentsForEvent(
        @PathVariable("jid") String jid,
        @PathVariable("ctid") String ctid,
        @PathVariable("cid") String cid) {
        try {
            return documentsOperation.getPrintableDocumentsForCase(jid, ctid, cid);
        } catch (NumberFormatException e) {
            throw new ApiException(String.format("Unrecognised Case Reference %s. Case Reference should be a number", cid));
        }
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @ApiOperation(value = "Get case data for a given case type")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search criteria")})
    public List<CaseDetails> searchCasesForCaseWorkers(@PathVariable("jid") final String jurisdictionId,
                                                      @PathVariable("ctid") final String caseTypeId,
                                                      @RequestParam Map<String, String> queryParameters) {
        return searchCases(jurisdictionId, caseTypeId, queryParameters);
    }

    @Transactional
    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @ApiOperation(value = "Get case data for a given case type")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search criteria")})
    public List<CaseDetails> searchCasesForCitizens(@PathVariable("jid") final String jurisdictionId,
                                                    @PathVariable("ctid") final String caseTypeId,
                                                    @RequestParam Map<String, String> queryParameters) {
        return searchCases(jurisdictionId, caseTypeId, queryParameters);
    }

    private List<CaseDetails> searchCases(final String jurisdictionId,
                                          final String caseTypeId,
                                          final Map<String, String> queryParameters) {

        final MetaData metadata = createMetadata(jurisdictionId, caseTypeId, queryParameters);

        final Map<String, String> sanitizedParams = fieldMapSanitizeOperation.execute(queryParameters);

        return searchOperation.execute(metadata, sanitizedParams);
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/pagination_metadata")
    @ApiOperation(value = "Get the pagination metadata for a case data search")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Pagination metadata for the given search criteria")})
    public PaginatedSearchMetadata searchCasesMetadataForCaseworkers(@PathVariable("jid") final String jurisdictionId,
                                                                  @PathVariable("ctid") final String caseTypeId,
                                                                  @RequestParam Map<String, String> queryParameters) {
        return searchMetadata(jurisdictionId, caseTypeId, queryParameters);
    }

    @Transactional
    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/pagination_metadata")
    @ApiOperation(value = "Get the pagination metadata for a case data search")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Pagination metadata for the given search criteria")})
    public PaginatedSearchMetadata searchCasesMetadataForCitizens(@PathVariable("jid") final String jurisdictionId,
                                                                  @PathVariable("ctid") final String caseTypeId,
                                                                  @RequestParam Map<String, String> queryParameters) {
        return searchMetadata(jurisdictionId, caseTypeId, queryParameters);
    }

    private PaginatedSearchMetadata searchMetadata(final String jurisdictionId,
                                                   final String caseTypeId,
                                                   final Map<String, String> queryParameters) {

        final MetaData metadata = createMetadata(jurisdictionId, caseTypeId, queryParameters);

        final Map<String, String> sanitizedParams = fieldMapSanitizeOperation.execute(queryParameters);

        return paginatedSearchMetaDataOperation.execute(metadata, sanitizedParams);
    }



    private void validateMetadataSearchParameters(Map<String, String> queryParameters) {
        List<String> metadataParams = queryParameters.keySet().stream().filter(p -> !FieldMapSanitizeOperation.isCaseFieldParameter(p)).collect(toList());
        if (!MetaData.unknownMetadata(metadataParams).isEmpty()) {
            throw new BadRequestException(String.format("unknown metadata search parameters: %s",
                    String.join((","), MetaData.unknownMetadata(metadataParams))));
        }
        param(queryParameters, SECURITY_CLASSIFICATION.getParameterName()).ifPresent(sc -> {
            if (!EnumUtils.isValidEnum(SecurityClassification.class, sc.toUpperCase())) {
                throw new BadRequestException(String.format("unknown security classification '%s'", sc));
            }
        });

        param(queryParameters, SORT_PARAM).ifPresent(sd -> {
            if (Stream.of("ASC", "DESC").noneMatch(direction -> direction.equalsIgnoreCase(sd))) {
                throw new BadRequestException(String.format("Unknown sort direction: %s", sd));
            }
        });
    }

    private Optional<String> param(Map<String, String> queryParameters, String param) {
        return Optional.ofNullable(queryParameters.get(param));
    }

    private MetaData createMetadata(String jurisdictionId, String caseTypeId, Map<String, String> queryParameters) {

        validateMetadataSearchParameters(queryParameters);

        final MetaData metadata = new MetaData(caseTypeId, jurisdictionId);
        metadata.setState(param(queryParameters, STATE.getParameterName()));
        metadata.setCaseReference(param(queryParameters, CASE_REFERENCE.getParameterName()));
        metadata.setCreatedDate(param(queryParameters, CREATED_DATE.getParameterName()));
        metadata.setLastModified(param(queryParameters, LAST_MODIFIED_DATE.getParameterName()));
        metadata.setSecurityClassification(param(queryParameters, SECURITY_CLASSIFICATION.getParameterName()));
        metadata.setPage(param(queryParameters, PAGE_PARAM));
        metadata.setSortDirection(param(queryParameters, SORT_PARAM));

        return metadata;
    }
}
