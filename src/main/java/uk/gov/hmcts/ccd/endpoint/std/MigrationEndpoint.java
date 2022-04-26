package uk.gov.hmcts.ccd.endpoint.std;

import com.google.common.collect.Iterables;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationResult;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkMigrationService;
import uk.gov.hmcts.ccd.domain.service.search.DefaultSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(name = "Migration API", path = "/migration")
@ConditionalOnProperty(value = "migrations.endpoint.enabled", havingValue = "true", matchIfMissing = false)
public class MigrationEndpoint {

    private final CaseLinkMigrationService caseLinkMigrationService;

    private final SearchOperation searchOperation;

    private final ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Autowired
    public MigrationEndpoint(final CaseLinkMigrationService caseLinkMigrationService,
                             @Qualifier(DefaultSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                             final ElasticsearchQueryHelper elasticsearchQueryHelper) {
        this.caseLinkMigrationService = caseLinkMigrationService;
        this.searchOperation = searchOperation;

        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
    }


    @PostMapping(path = "/populateCaseLinks", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(
        value = "Migrate case links",
        notes = "Updates the case link records for the cases found using the migration properties")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Migration completed for range of records specified"),
        @ApiResponse(code = 403, message = "User does not have access to CaseType"),
    })
    @LogAudit(
        operationType = AuditOperationType.MIGRATION,
        caseId = "#migrationParameters.caseDataId",
        jurisdiction = "#migrationParameters.jurisdictionId",
        caseType = "#migrationParameters.caseTypeId"
    )
    public MigrationResult backPopulateCaseLinks(@RequestBody MigrationParameters migrationParameters) {

        List<CaseDetails> caseDetails = searchCases(migrationParameters);

        caseLinkMigrationService.backPopulateCaseLinkTable(caseDetails);

        return generateMigrationResult(caseDetails);
    }

    private List<CaseDetails> searchCases(MigrationParameters migrationParameters) {
        List<String> caseTypesAvailableToUser = elasticsearchQueryHelper.getCaseTypesAvailableToUser();

        // migration is using DefaultSearchOperation rather than AuthorisedSearchOperation or ClassifiedSearchOperation
        // ... therefore we will verify the user has access to the case type as a simple fallback.
        if (!caseTypesAvailableToUser.contains(migrationParameters.getCaseTypeId())) {
            log.error("User does not have access to CaseType: '{}'", migrationParameters.getCaseTypeId());
            throw new ForbiddenException();
        }

        return searchOperation.execute(migrationParameters);
    }

    private MigrationResult generateMigrationResult(List<CaseDetails> caseDetails) {
        MigrationResult migrationResult = new MigrationResult();

        if (!caseDetails.isEmpty()) {
            migrationResult.setRecordCount(caseDetails.size());
            migrationResult.setFinalRecordId(Integer.parseInt(Iterables.getLast(caseDetails).getId()));
        }

        return migrationResult;
    }

}
