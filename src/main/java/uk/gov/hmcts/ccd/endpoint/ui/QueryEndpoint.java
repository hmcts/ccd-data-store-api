package uk.gov.hmcts.ccd.endpoint.ui;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.PAGE_PARAM;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.SORT_PARAM;
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
    private final GetCaseViewOperation getDraftViewOperation;
    private final GetCaseHistoryViewOperation getCaseHistoryViewOperation;
    private final GetEventTriggerOperation getEventTriggerOperation;
    private final SearchQueryOperation searchQueryOperation;
    private final FieldMapSanitizeOperation fieldMapSanitizeOperation;
    private final FindSearchInputOperation findSearchInputOperation;
    private final FindWorkbasketInputOperation findWorkbasketInputOperation;
    private final GetCaseTypesOperation getCaseTypesOperation;
    private final HashMap<String, Predicate<AccessControlList>> accessMap;

    @Inject
    public QueryEndpoint(
        @Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(DefaultGetCaseViewFromDraftOperation.QUALIFIER) GetCaseViewOperation getDraftViewOperation,
        @Qualifier(AuthorisedGetCaseHistoryViewOperation.QUALIFIER) GetCaseHistoryViewOperation getCaseHistoryOperation,
        @Qualifier(AuthorisedGetEventTriggerOperation.QUALIFIER) GetEventTriggerOperation getEventTriggerOperation,
        SearchQueryOperation searchQueryOperation, FieldMapSanitizeOperation fieldMapSanitizeOperation,
        @Qualifier(AuthorisedFindSearchInputOperation.QUALIFIER) FindSearchInputOperation findSearchInputOperation,
        @Qualifier(
            AuthorisedFindWorkbasketInputOperation.QUALIFIER) FindWorkbasketInputOperation findWorkbasketInputOperation,
        @Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER) GetCaseTypesOperation getCaseTypesOperation) {

        this.getCaseViewOperation = getCaseViewOperation;
        this.getDraftViewOperation = getDraftViewOperation;
        this.getCaseHistoryViewOperation = getCaseHistoryOperation;
        this.getEventTriggerOperation = getEventTriggerOperation;
        this.searchQueryOperation = searchQueryOperation;
        this.fieldMapSanitizeOperation = fieldMapSanitizeOperation;
        this.findSearchInputOperation = findSearchInputOperation;
        this.findWorkbasketInputOperation = findWorkbasketInputOperation;
        this.getCaseTypesOperation = getCaseTypesOperation;
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
    public List<CaseType> getCaseTypes(@PathVariable("jid") final String jurisdictionId,
                                       @RequestParam(value = "access", required = true) String access) {
        return getCaseTypesOperation.execute(jurisdictionId, ofNullable(accessMap.get(access))
            .orElseThrow(() -> new ResourceNotFoundException("No case types found")));
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases",
        method = RequestMethod.GET)
    @ApiOperation(value = "Get case data with UI layout")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search criteria"),
        @ApiResponse(code = 412, message = "Mismatch between case type and workbasket definitions")})
    public SearchResultView searchNew(@PathVariable("jid") final String jurisdictionId,
                                      @PathVariable("ctid") final String caseTypeId,
                                      @RequestParam java.util.Map<String, String> params) {
        String view = params.get("view");
        MetaData metadata = new MetaData(caseTypeId, jurisdictionId);
        metadata.setState(param(params, STATE.getParameterName()));
        metadata.setCaseReference(param(params, CASE_REFERENCE.getParameterName()));
        metadata.setCreatedDate(param(params, CREATED_DATE.getParameterName()));
        metadata.setLastModified(param(params, LAST_MODIFIED_DATE.getParameterName()));
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
        return findSearchInputOperation.execute(jurisdictionId, caseTypeId, CAN_READ).toArray(new SearchInput[0]);
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
        WorkbasketInput[] workbasketInputs = findWorkbasketInputOperation.execute(jurisdictionId, caseTypeId,
                                                                                  CAN_READ).toArray(
            new WorkbasketInput[0]);
        final Duration between = Duration.between(start, Instant.now());
        LOG.warn("findWorkbasketInputDetails has been completed in {} millisecs...", between.toMillis());
        return workbasketInputs;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch a case for display")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A displayable case")
    })
    public CaseView findCase(@PathVariable("jid") final String jurisdictionId,
                             @PathVariable("ctid") final String caseTypeId,
                             @PathVariable("cid") final String cid) {
        Instant start = Instant.now();
        CaseView caseView = getCaseViewOperation.execute(jurisdictionId, caseTypeId, cid);
        final Duration between = Duration.between(start, Instant.now());
        LOG.warn("findCase has been completed in {} millisecs...", between.toMillis());
        return caseView;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch a draft for display")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A displayable draft")
    })
    public CaseView findDraft(@PathVariable("jid") final String jurisdictionId,
                              @PathVariable("ctid") final String caseTypeId,
                              @PathVariable("did") final String did) {
        Instant start = Instant.now();
        CaseView caseView = getDraftViewOperation.execute(jurisdictionId, caseTypeId, did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.warn("findDraft has been completed in {} millisecs...", between.toMillis());
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
    public CaseEventTrigger getEventTriggerForCaseType(@PathVariable("uid") String userId,
                                                       @PathVariable("jid") String jurisdictionId,
                                                       @PathVariable("ctid") String casetTypeId,
                                                       @PathVariable("etid") String eventTriggerId,
                                                       @RequestParam(value = "ignore-warning",
                                                           required = false) Boolean ignoreWarning) {
        return getEventTriggerOperation.executeForCaseType(userId,
                                                           jurisdictionId,
                                                           casetTypeId,
                                                           eventTriggerId,
                                                           ignoreWarning);
    }

    @Transactional
    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch an event trigger in the context of a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Valid pre-state conditions")
    })
    public CaseEventTrigger getEventTriggerForCase(@PathVariable("uid") String userId,
                                                   @PathVariable("jid") String jurisdictionId,
                                                   @PathVariable("ctid") String caseTypeId,
                                                   @PathVariable("cid") String caseId,
                                                   @PathVariable("etid") String eventTriggerId,
                                                   @RequestParam(value = "ignore-warning",
                                                       required = false) Boolean ignoreWarning) {
        return getEventTriggerOperation.executeForCase(userId,
                                                       jurisdictionId,
                                                       caseTypeId,
                                                       caseId,
                                                       eventTriggerId,
                                                       ignoreWarning);
    }

    @Transactional
    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}/event-triggers/{etid}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch an event trigger in the context of a case")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Valid pre-state conditions")
    })
    public CaseEventTrigger getEventTriggerForDraft(@PathVariable("uid") String userId,
                                                    @PathVariable("jid") String jurisdictionId,
                                                    @PathVariable("ctid") String caseTypeId,
                                                    @PathVariable("did") String draftId,
                                                    @PathVariable("etid") String eventTriggerId,
                                                    @RequestParam(value = "ignore-warning",
                                                        required = false) Boolean ignoreWarning) {
        return getEventTriggerOperation.executeForDraft(userId,
                                                        jurisdictionId,
                                                        caseTypeId,
                                                        draftId,
                                                        eventTriggerId,
                                                        ignoreWarning);
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
        CaseHistoryView caseView = getCaseHistoryViewOperation.execute(jurisdictionId, caseTypeId, caseReference,
                                                                       eventId);
        final Duration between = Duration.between(start, Instant.now());
        LOG.warn("getCaseHistoryForEvent has been completed in {} millisecs...", between.toMillis());
        return caseView;
    }

}
