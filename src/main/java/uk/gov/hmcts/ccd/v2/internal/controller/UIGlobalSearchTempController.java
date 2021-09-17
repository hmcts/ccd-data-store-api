package uk.gov.hmcts.ccd.v2.internal.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteriaResponseTEMP;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;

import java.util.List;

@RestController
@RequestMapping(path = "/internal/searchCases/global", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class UIGlobalSearchTempController {

    private final GlobalSearchParser globalSearchParser;

    @Autowired
    @SuppressWarnings("checkstyle:LineLength") //don't want to break message

    public UIGlobalSearchTempController(
        GlobalSearchParser globalSearchParser) {
        this.globalSearchParser = globalSearchParser;
    }

    @Transactional
    @PostMapping(path = "")
    @ApiOperation(
        value = "Search cases according to the provided ElasticSearch query. Supports searching a single case type and"
            + " a use case."
    )
    @SuppressWarnings("checkstyle:LineLength") // don't want to break message
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE)
    public ResponseEntity<List<CaseDetails>> searchCases(@RequestBody SearchCriteriaResponseTEMP values) {
        List<CaseDetails> response = globalSearchParser.filterCases(values.getResponse(), values.getRequestValues());

        return ResponseEntity.ok(response);
    }
}
