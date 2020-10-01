package uk.gov.hmcts.ccd.endpoint.ui;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseTypesOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypesOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_STATE_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.PAGE_PARAM;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.SORT_PARAM;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@RestController
@RequestMapping(path = "/aggregated",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class QueryEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(QueryEndpoint.class);

    private final GetCaseViewOperation getCaseViewOperation;
    private final GetCaseHistoryViewOperation getCaseHistoryViewOperation;
    private final GetEventTriggerOperation getEventTriggerOperation;
    private final SearchQueryOperation searchQueryOperation;
    private final FieldMapSanitizeOperation fieldMapSanitizeOperation;
    private final GetCriteriaOperation getCriteriaOperation;
    private final GetCaseTypesOperation getCaseTypesOperation;
    private final GetUserProfileOperation getUserProfileOperation;

    private final HashMap<String, Predicate<AccessControlList>> accessMap;

    @Inject
    public QueryEndpoint(
        @Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(AuthorisedGetCaseHistoryViewOperation.QUALIFIER) GetCaseHistoryViewOperation getCaseHistoryOperation,
        @Qualifier(AuthorisedGetEventTriggerOperation.QUALIFIER) GetEventTriggerOperation getEventTriggerOperation,
        SearchQueryOperation searchQueryOperation, FieldMapSanitizeOperation fieldMapSanitizeOperation,
        @Qualifier(AuthorisedGetCriteriaOperation.QUALIFIER) GetCriteriaOperation getCriteriaOperation,
        @Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER) GetCaseTypesOperation getCaseTypesOperation,
        @Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER) final GetUserProfileOperation getUserProfileOperation) {

        this.getCaseViewOperation = getCaseViewOperation;
        this.getCaseHistoryViewOperation = getCaseHistoryOperation;
        this.getEventTriggerOperation = getEventTriggerOperation;
        this.searchQueryOperation = searchQueryOperation;
        this.fieldMapSanitizeOperation = fieldMapSanitizeOperation;
        this.getCriteriaOperation = getCriteriaOperation;
        this.getCaseTypesOperation = getCaseTypesOperation;
        this.getUserProfileOperation = getUserProfileOperation;
        this.accessMap = Maps.newHashMap();
        accessMap.put("create", CAN_CREATE);
        accessMap.put("update", CAN_UPDATE);
        accessMap.put("read", CAN_READ);
    }

    /*
     * @deprecated see https://tools.hmcts.net/jira/browse/RDM-1421
     */
    @Deprecated
    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types", method = RequestMethod.GET)
    @ApiOperation(value = "Get case types")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case types for the given access criteria"),
        @ApiResponse(code = 404, message = "No case types found for given access criteria")})
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public List<CaseTypeDefinition> getCaseTypes(@PathVariable("jid") final String jurisdictionId,
                                                 @RequestParam(value = "access", required = true) String access) {
        return getCaseTypesOperation.execute(jurisdictionId, ofNullable(accessMap.get(access))
            .orElseThrow(() -> new ResourceNotFoundException("No case types found")));
    }

    @GetMapping(value = "/caseworkers/{uid}/jurisdictions")
    @ApiOperation(value = "Get jurisdictions available to the user")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of jurisdictions for the given access criteria"),
        @ApiResponse(code = 404, message = "No jurisdictions found for given access criteria")})
    public List<JurisdictionDisplayProperties> getJurisdictions(@RequestParam(value = "access") String access) {
        if (accessMap.get(access) == null) {
            throw new BadRequestException("Access can only be 'create', 'read' or 'update'");
        }
        List<JurisdictionDisplayProperties> jurisdictions = Arrays.asList(
            getUserProfileOperation.execute(accessMap.get(access)).getJurisdictions());
        if (jurisdictions.isEmpty()) {
            throw new ResourceNotFoundException("No jurisdictions found");
        } else {
            return jurisdictions;
        }
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases",
        method = RequestMethod.GET)
    @ApiOperation(value = "Get case data with UI layout")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search criteria"),
        @ApiResponse(code = 412, message = "Mismatch between case type and workbasket definitions")})
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId", caseId = "T(uk.gov.hmcts.ccd.endpoint.ui.QueryEndpoint).buildCaseIds(#result)")
    public SearchResultView searchNew(@PathVariable("jid") final String jurisdictionId,
                                      @PathVariable("ctid") final String caseTypeId,
                                      @RequestParam java.util.Map<String, String> params) {
        final String view = params.get("view");
        MetaData metadata = new MetaData(caseTypeId, jurisdictionId);
        metadata.setState(param(params, STATE.getParameterName()));
        metadata.setCaseReference(param(params, CASE_REFERENCE.getParameterName()));
        metadata.setCreatedDate(param(params, CREATED_DATE.getParameterName()));
        metadata.setLastModifiedDate(param(params, LAST_MODIFIED_DATE.getParameterName()));
        metadata.setLastStateModifiedDate(param(params, LAST_STATE_MODIFIED_DATE.getParameterName()));
        metadata.setSecurityClassification(param(params, SECURITY_CLASSIFICATION.getParameterName()));
        metadata.setPage(param(params, PAGE_PARAM));
        metadata.setSortDirection(param(params, SORT_PARAM));

        Map<String, String> sanitized = fieldMapSanitizeOperation.execute(params);

        return searchQueryOperation.execute(view, metadata, sanitized);
    }

    private Optional<String> param(Map<String, String> queryParameters, String param) {
        return Optional.ofNullable(queryParameters.get(param));
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/inputs",
        method = RequestMethod.GET)
    @ApiOperation(value = "Get Search Input details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Search Input data found for the given case type and jurisdiction"),
        @ApiResponse(code = 404, message = "No SearchInput found for the given case type and jurisdiction")
    })
    public SearchInput[] findSearchInputDetails(@PathVariable("uid") final String uid,
                                                @PathVariable("jid") final String jurisdictionId,
                                                @PathVariable("ctid") final String caseTypeId) {
        return getCriteriaOperation
            .execute(caseTypeId, CAN_READ, SEARCH)
            .toArray(new SearchInput[0]);
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/work-basket-inputs",
        method = RequestMethod.GET)
    @ApiOperation(value = "Get Workbasket Input details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Workbasket Input data found for the given case type and jurisdiction"),
        @ApiResponse(code = 404, message = "No Workbasket Input found for the given case type and jurisdiction")
    })
    public WorkbasketInput[] findWorkbasketInputDetails(@PathVariable("uid") final String uid,
                                                        @PathVariable("jid") final String jurisdictionId,
                                                        @PathVariable("ctid") final String caseTypeId) {
        Instant start = Instant.now();
        WorkbasketInput[] workbasketInputs = getCriteriaOperation
            .execute(caseTypeId, CAN_READ, WORKBASKET)
            .toArray(new WorkbasketInput[0]);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("findWorkbasketInputDetails has been completed in {} millisecs...", between.toMillis());
        return workbasketInputs;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch a case for display")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A displayable case")
    })
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId", caseId = "#cid")
    public CaseView findCase(@PathVariable("jid") final String jurisdictionId,
                             @PathVariable("ctid") final String caseTypeId,
                             @PathVariable("cid") final String cid) {
        Instant start = Instant.now();
        CaseView caseView = getCaseViewOperation.execute(cid);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("findCase has been completed in {} millisecs...", between.toMillis());
        return caseView;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-triggers/{etid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch an event trigger in the context of a case type")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Empty pre-state conditions"),
        @ApiResponse(code = 422, message = "The case status did not qualify for the event")
    })
    public CaseUpdateViewEvent getEventTriggerForCaseType(@PathVariable("uid") String userId,
                                                          @PathVariable("jid") String jurisdictionId,
                                                          @PathVariable("ctid") String caseTypeId,
                                                          @PathVariable("etid") String eventTriggerId,
                                                          @RequestParam(value = "ignore-warning",
                                                           required = false) Boolean ignoreWarning) {
        return getEventTriggerOperation.executeForCaseType(caseTypeId, eventTriggerId, ignoreWarning);
    }

    @Transactional
    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch an event trigger in the context of a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Valid pre-state conditions")
    })
    public CaseUpdateViewEvent getEventTriggerForCase(@PathVariable("uid") String userId,
                                                      @PathVariable("jid") String jurisdictionId,
                                                      @PathVariable("ctid") String caseTypeId,
                                                      @PathVariable("cid") String caseId,
                                                      @PathVariable("etid") String eventId,
                                                      @RequestParam(value = "ignore-warning",
                                                       required = false) Boolean ignoreWarning) {
        return getEventTriggerOperation.executeForCase(caseId, eventId, ignoreWarning);
    }

    @Transactional
    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}/event-triggers/{etid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch an event trigger in the context of a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Valid pre-state conditions")
    })
    public CaseUpdateViewEvent getEventTriggerForDraft(@PathVariable("uid") String userId,
                                                       @PathVariable("jid") String jurisdictionId,
                                                       @PathVariable("ctid") String caseTypeId,
                                                       @PathVariable("did") String draftId,
                                                       @PathVariable("etid") String eventId,
                                                       @RequestParam(value = "ignore-warning",
                                                        required = false) Boolean ignoreWarning) {
        return getEventTriggerOperation.executeForDraft(draftId, ignoreWarning);
    }

    @Transactional
    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events/{eventId}/case-history",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch case history for the event")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Displayable case data"),
        @ApiResponse(code = 404, message = "Invalid jurisdiction/case type/case reference or event id")
    })
    public CaseHistoryView getCaseHistoryForEvent(@PathVariable("jid") final String jurisdictionId,
                                                  @PathVariable("ctid") final String caseTypeId,
                                                  @PathVariable("cid") final String caseReference,
                                                  @PathVariable("eventId") final Long eventId) {
        Instant start = Instant.now();
        CaseHistoryView caseView = getCaseHistoryViewOperation.execute(caseReference, eventId);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("getCaseHistoryForEvent has been completed in {} millisecs...", between.toMillis());
        return caseView;
    }

    public static String buildCaseIds(SearchResultView searchResultView) {
        return searchResultView.getSearchResultViewItems().stream().limit(MAX_CASE_IDS_LIST)
            .map(c -> c.getCaseId())
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

}
