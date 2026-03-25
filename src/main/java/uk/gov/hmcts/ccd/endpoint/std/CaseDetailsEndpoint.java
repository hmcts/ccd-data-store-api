package uk.gov.hmcts.ccd.endpoint.std;

import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.PaginatedSearchMetaDataOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.domain.service.stdapi.DocumentsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.AuthorisedValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.OperationContext;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.PAGE_PARAM;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.SORT_PARAM;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_STATE_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Standard case API")
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

    @Autowired
    public CaseDetailsEndpoint(@Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                               @Qualifier("authorised") final CreateCaseOperation createCaseOperation,
                               @Qualifier("authorised") final CreateEventOperation createEventOperation,
                               @Qualifier("authorised") final StartEventOperation startEventOperation,
                               @Qualifier(AuthorisedSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                               final FieldMapSanitizeOperation fieldMapSanitizeOperation,
                               @Qualifier(AuthorisedValidateCaseFieldsOperation.QUALIFIER)
                               final ValidateCaseFieldsOperation validateCaseFieldsOperation,
                               final DocumentsOperation documentsOperation,
                               final PaginatedSearchMetaDataOperation paginatedSearchMetaDataOperation,
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
        this.appInsights = appinsights;
    }

    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}")
    @Operation(summary = "Get case", description = "Retrieve an existing case with its state and data")
    @ApiResponse(responseCode = "200", description = "Case found for the given ID")
    @ApiResponse(responseCode = "400", description = "Invalid case ID")
    @ApiResponse(responseCode = "404", description = "No case found for the given ID")
    @LogAudit(operationType = AuditOperationType.CASE_ACCESSED, caseId = "#caseId", jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId")
    public CaseDetails findCaseDetailsForCaseworker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId) {

        final Instant start = Instant.now();
        final CaseDetails caseDetails = getCaseOperation.execute(jurisdictionId, caseTypeId, caseId)
            .orElseThrow(() -> new CaseNotFoundException(jurisdictionId, caseTypeId, caseId));
        final Duration duration = Duration.between(start, Instant.now());
        appInsights.trackRequest("findCaseDetailsForCaseworker", duration.toMillis(), true);
        return caseDetails;
    }

    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}")
    @Operation(summary = "Get case", description = "Retrieve an existing case with its state and data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case found for the given ID"),
        @ApiResponse(responseCode = "400", description = "Invalid case ID"),
        @ApiResponse(responseCode = "404", description = "No case found for the given ID")
    })
    @LogAudit(operationType = AuditOperationType.CASE_ACCESSED, caseId = "#caseId", jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId")
    public CaseDetails findCaseDetailsForCitizen(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId) {

        return getCaseOperation.execute(jurisdictionId, caseTypeId, caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    @GetMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}/token")
    @Operation(
        summary = "Start event creation as Case worker",
        description = "Start the event creation process for an existing case. Triggers `AboutToStart` callback."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event creation process started"),
        @ApiResponse(responseCode = "404", description = "No case found for the given ID"),
        @ApiResponse(responseCode = "422", description = "Process could not be started")
    })
    public StartEventResult startEventForCaseworker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @Parameter(name = "Event ID", required = true)
        @PathVariable("etid") final String eventId,
        @Parameter(name = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCase(caseId, eventId, ignoreWarning);
    }

    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}/token")
    @Operation(
        summary = "Start event creation as Citizen",
        description = "Start the event creation process for an existing case. Triggers `AboutToStart` callback."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event creation process started"),
        @ApiResponse(responseCode = "404", description = "No case found for the given ID"),
        @ApiResponse(responseCode = "422", description = "Process could not be started")
    })
    public StartEventResult startEventForCitizen(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @Parameter(name = "Event ID", required = true)
        @PathVariable("etid") final String eventId,
        @Parameter(name = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCase(caseId, eventId, ignoreWarning);
    }

    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-triggers/{etid}/token")
    @Operation(
        summary = "Start case creation as Case worker",
        description = "Start the case creation process for a new case. Triggers `AboutToStart` callback."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case creation process started"),
        @ApiResponse(responseCode = "422", description = "Process could not be started")
    })
    public StartEventResult startCaseForCaseworker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Event ID", required = true)
        @PathVariable("etid") final String eventId,
        @Parameter(name = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCaseType(caseTypeId, eventId, ignoreWarning);
    }

    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-triggers/{etid}/token")
    @Operation(
        summary = "Start case creation as Citizen",
        description = "Start the case creation process for a new case. Triggers `AboutToStart` callback."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case creation process started"),
        @ApiResponse(responseCode = "422", description = "Process could not be started")
    })
    public StartEventResult startCaseForCitizen(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Event ID", required = true)
        @PathVariable("etid") final String eventId,
        @Parameter(name = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        return startEventOperation.triggerStartForCaseType(caseTypeId, eventId, ignoreWarning);
    }

    @PostMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Submit case creation as Case worker",
        description = "Complete the case creation process. This requires a valid event token to be provided, "
            + "as generated by `startCaseForCaseworker`."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case created"),
        @ApiResponse(responseCode = "422", description = "Case submission failed"),
        @ApiResponse(responseCode = "409", description = "Case reference not unique")
    })
    @LogAudit(operationType = AuditOperationType.CREATE_CASE, caseId = "#result.reference",
        jurisdiction = "#result.jurisdiction", caseType = "#caseTypeId", eventName = "#content.event.eventId")
    public CaseDetails saveCaseDetailsForCaseWorker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Should `AboutToSubmit` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning,
        @RequestBody final CaseDataContent content) {

        return createCaseOperation.createCaseDetails(caseTypeId, content, ignoreWarning);
    }

    @PostMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Submit case creation as Citizen",
        description = "Complete the case creation process. This requires a valid event token to be provided, "
            + "as generated by `startCaseForCitizen`."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case created"),
        @ApiResponse(responseCode = "422", description = "Case submission failed"),
        @ApiResponse(responseCode = "409", description = "Case reference not unique")
    })
    @LogAudit(operationType = AuditOperationType.CREATE_CASE, caseId = "#result.reference",
        jurisdiction = "#result.jurisdiction", caseType = "#caseTypeId", eventName = "#content.event.eventId")
    public CaseDetails saveCaseDetailsForCitizen(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Should `AboutToSubmit` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning,
        @RequestBody final CaseDataContent content) {

        return createCaseOperation.createCaseDetails(caseTypeId, content, ignoreWarning);
    }

    @PostMapping(value = {"/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/validate",
        "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/validate"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Validate a set of fields as Case worker",
        description = "Validate the case data entered during the case/event creation process."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation OK"),
        @ApiResponse(responseCode = "422", description = "Field validation failed"),
        @ApiResponse(responseCode = "409", description = "Case reference not unique")
    })
    public JsonNode validateCaseDetails(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Page ID")
        @RequestParam(required = false) final String pageId,
        @RequestBody final CaseDataContent content) {

        validateCaseFieldsOperation.validateCaseDetails(new OperationContext(caseTypeId, content, pageId));
        return JacksonUtils.convertValueJsonNode(content.getData());
    }

    @PostMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Submit event creation as Case worker",
        description = "Complete the event creation process. This requires a valid event token to be provided, "
            + "as generated by `startEventForCaseworker`."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case event created"),
        @ApiResponse(responseCode = "409", description = "Case altered outside of transaction"),
        @ApiResponse(responseCode = "422", description = "Event submission failed")
    })
    @LogAudit(operationType = AuditOperationType.UPDATE_CASE, caseId = "#caseId", jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId", eventName = "#content.event.eventId")
    public CaseDetails createCaseEventForCaseWorker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @RequestBody final CaseDataContent content) {
        return createEventOperation.createCaseEvent(caseId,
                                                    content);
    }

    @PostMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Submit event creation as Citizen",
        description = "Complete the event creation process. This requires a valid event token to be provided, "
            + "as generated by `startEventForCitizen`."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case event created"),
        @ApiResponse(responseCode = "409", description = "Case altered outside of transaction"),
        @ApiResponse(responseCode = "422", description = "Event submission failed")
    })
    @LogAudit(operationType = AuditOperationType.UPDATE_CASE, caseId = "#caseId", jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId", eventName = "#content.event.eventId")
    public CaseDetails createCaseEventForCitizen(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @RequestBody final CaseDataContent content) {
        return createEventOperation.createCaseEvent(caseId,
                                                    content);
    }

    /**
     * Gets printable documents for a case (deprecated).
     *
     * @deprecated This endpoint is deprecated and will be removed in a future release.
     *     Use {@code /{caseId}/documents} instead.
     */
    @Deprecated
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/documents")
    @Operation(
        summary = "Get a list of printable documents for the given case id",
        deprecated = true,
        description = "Deprecated. Use /{caseId}/documents instead."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documents list for the given case id")
    })
    public List<Document> getDocumentsForCase(
        @PathVariable("uid") final String uid,
        @PathVariable("jid") String jid,
        @PathVariable("ctid") String ctid,
        @PathVariable("cid") String cid) {
        try {
            return documentsOperation.getPrintableDocumentsForCase(cid);
        } catch (NumberFormatException e) {
            throw new ApiException(String.format("Unrecognised Case Reference %s. Case Reference should be a number",
                cid));
        }
    }

    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @Operation(summary = "Get case data for a given case type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of case data for the given search criteria")})
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId", caseId = "T(uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint).buildCaseIds(#result)")
    public List<CaseDetails> searchCasesForCaseWorkers(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Query Parameters",
            description = "Query Parameters, valid options: created_date, last_modified_date, "
            + "state, case_reference", required = false)
        @RequestParam(required = false) Map<String, String> queryParameters) {
        return searchCases(jurisdictionId, caseTypeId, queryParameters);
    }

    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases")
    @Operation(summary = "Get case data for a given case type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of case data for the given search criteria")})
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId", caseId = "T(uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint).buildCaseIds(#result)")
    public List<CaseDetails> searchCasesForCitizens(@PathVariable("uid") final String uid,
                                                    @PathVariable("jid") final String jurisdictionId,
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

    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/pagination_metadata")
    @Operation(summary = "Get the pagination metadata for a case data search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pagination metadata for the given search criteria")})
    public PaginatedSearchMetadata searchCasesMetadataForCaseworkers(@PathVariable("uid") final String uid,
                                                                     @PathVariable("jid") final String jurisdictionId,
                                                                     @PathVariable("ctid") final String caseTypeId,
                                                                     @RequestParam Map<String, String>
                                                                             queryParameters) {
        return searchMetadata(jurisdictionId, caseTypeId, queryParameters);
    }

    @GetMapping(value = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/pagination_metadata")
    @Operation(summary = "Get the pagination metadata for a case data search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pagination metadata for the given search criteria")})
    public PaginatedSearchMetadata searchCasesMetadataForCitizens(@PathVariable("uid") final String uid,
                                                                  @PathVariable("jid") final String jurisdictionId,
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
        List<String> metadataParams = queryParameters.keySet().stream().filter(p ->
            !FieldMapSanitizeOperation.isCaseFieldParameter(p)).collect(toList());
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
        metadata.setLastModifiedDate(param(queryParameters, LAST_MODIFIED_DATE.getParameterName()));
        metadata.setLastStateModifiedDate(param(queryParameters, LAST_STATE_MODIFIED_DATE.getParameterName()));
        metadata.setSecurityClassification(param(queryParameters, SECURITY_CLASSIFICATION.getParameterName()));
        metadata.setPage(param(queryParameters, PAGE_PARAM));
        metadata.setSortDirection(param(queryParameters, SORT_PARAM));

        return metadata;
    }

    public static String buildCaseIds(List<CaseDetails> caseDetails) {
        return caseDetails.stream().limit(MAX_CASE_IDS_LIST)
            .map(c -> String.valueOf(c.getReference()))
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }
}
