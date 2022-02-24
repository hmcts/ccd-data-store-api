package uk.gov.hmcts.ccd.endpoint.std;

import com.google.common.collect.Iterables;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationResult;
import uk.gov.hmcts.ccd.domain.service.migration.CaseLinkMigrationService;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/migration",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Migration API")
//@ConditionalOnProperty(value = "case.link.migration.enabled", havingValue = "true")
public class MigrationEndpoint {

    private final CaseLinkMigrationService caseLinkMigrationService;

    private final SearchOperation searchOperation;
    private final FieldMapSanitizeOperation fieldMapSanitizeOperation;

    private final ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Autowired
    public MigrationEndpoint(final CaseLinkMigrationService caseLinkMigrationService,
                             @Qualifier(AuthorisedSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                             final FieldMapSanitizeOperation fieldMapSanitizeOperation,
                             final ElasticsearchQueryHelper elasticsearchQueryHelper) {
        this.caseLinkMigrationService = caseLinkMigrationService;
        this.searchOperation = searchOperation;
        this.fieldMapSanitizeOperation = fieldMapSanitizeOperation;

        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
    }

    @Transactional
    @RequestMapping(
        value = "/populateCaseLinks",
        method = RequestMethod.POST
        //consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(value = "Update case with case links", notes = "Update cases with case link values if they dont exist")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Events found for the given ID"),
        @ApiResponse(code = 400, message = "Invalid case ID"),
        @ApiResponse(code = 404, message = "No case found for the given ID")
    })
    @LogAudit(
        operationType = AuditOperationType.MIGRATION,
        caseId = "#migrationParameters.caseDataId",
        jurisdiction = "#migrationParameters.jurisdictionId",
        caseType = "#migrationParameters.caseTypeId")
    public MigrationResult backPopulateCaseLinks(
        @RequestBody MigrationParameters migrationParameters) {
        MigrationResult migrationResult = new MigrationResult();
        List<String> caseTypesAvailableToUser = elasticsearchQueryHelper.getCaseTypesAvailableToUser();

        if (caseTypesAvailableToUser.contains(migrationParameters.getCaseTypeId())) {
            List<CaseDetails> caseDetails = searchCases(migrationParameters);

            caseLinkMigrationService.backPopulateCaseLinkTable(caseDetails);

            if (!caseDetails.isEmpty()) {
                migrationResult.setRecordCount(caseDetails.size());
                migrationResult.setFinalRecordId(Integer.parseInt(Iterables.getLast(caseDetails).getId()));
            }

        }else {
            log.error("User does not have access to CaseType: '{}'", migrationParameters.getCaseDataId());
            throw new ForbiddenException();
        }
        return migrationResult;
    }

    private List<CaseDetails> searchCases(MigrationParameters migrationParameters) {
        return searchOperation.execute(migrationParameters);
    }

}
