package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.caselinks.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.migration.CaseLinkMigrationService;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_STATE_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.PAGE_PARAM;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.SORT_PARAM;

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
    @LogAudit(operationType = AuditOperationType.UPDATE_CASE, caseId = "#caseId", jurisdiction = "#jurisdictionId",
        caseType = "#caseTypeId")
    public void backPopulateCaseLinks(
        @RequestBody MigrationParameters migrationParameters) {

        List<String> caseTypesAvailableToUser = elasticsearchQueryHelper.getCaseTypesAvailableToUser();

        //if (caseTypesAvailableToUser.contains(migrationParameters.getCaseTypeId())) {
            List<CaseDetails> caseDetails = searchCases(migrationParameters);

            caseLinkMigrationService.backPopulateCaseLinkTable(caseDetails);
        //}
    }

//    private List<CaseDetails> searchCases(final String jurisdictionId,
//                                          final String caseTypeId,
//                                          final Map<String, String> queryParameters) {
//
//        final MetaData metadata = createMetadata(jurisdictionId, caseTypeId, queryParameters);
//
//        final Map<String, String> sanitizedParams = fieldMapSanitizeOperation.execute(queryParameters);
//
//        return searchOperation.execute(metadata, sanitizedParams);
//    }

    private List<CaseDetails> searchCases(MigrationParameters migrationParameters) {
        return searchOperation.execute(migrationParameters);
    }

//    private MetaData createMetadata(String jurisdictionId, String caseTypeId, Map<String, String> queryParameters) {
//
//        validateMetadataSearchParameters(queryParameters);
//
//        final MetaData metadata = new MetaData(caseTypeId, jurisdictionId);
//        metadata.setState(param(queryParameters, STATE.getParameterName()));
//        metadata.setCaseReference(param(queryParameters, CASE_REFERENCE.getParameterName()));
//        metadata.setCreatedDate(param(queryParameters, CREATED_DATE.getParameterName()));
//        metadata.setLastModifiedDate(param(queryParameters, LAST_MODIFIED_DATE.getParameterName()));
//        metadata.setLastStateModifiedDate(param(queryParameters, LAST_STATE_MODIFIED_DATE.getParameterName()));
//        metadata.setSecurityClassification(param(queryParameters, SECURITY_CLASSIFICATION.getParameterName()));
//        metadata.setPage(param(queryParameters, PAGE_PARAM));
//        metadata.setSortDirection(param(queryParameters, SORT_PARAM));
//
//        return metadata;
//    }
//
//    private void validateMetadataSearchParameters(Map<String, String> queryParameters) {
//        List<String> metadataParams = queryParameters.keySet().stream().filter(p ->
//            !FieldMapSanitizeOperation.isCaseFieldParameter(p)).collect(toList());
//        if (!MetaData.unknownMetadata(metadataParams).isEmpty()) {
//            throw new BadRequestException(String.format("unknown metadata search parameters: %s",
//                String.join((","), MetaData.unknownMetadata(metadataParams))));
//        }
//        param(queryParameters, SECURITY_CLASSIFICATION.getParameterName()).ifPresent(sc -> {
//            if (!EnumUtils.isValidEnum(SecurityClassification.class, sc.toUpperCase())) {
//                throw new BadRequestException(String.format("unknown security classification '%s'", sc));
//            }
//        });
//
//        param(queryParameters, SORT_PARAM).ifPresent(sd -> {
//            if (Stream.of("ASC", "DESC").noneMatch(direction -> direction.equalsIgnoreCase(sd))) {
//                throw new BadRequestException(String.format("Unknown sort direction: %s", sd));
//            }
//        });
//    }
//
//    private Optional<String> param(Map<String, String> queryParameters, String param) {
//        return Optional.ofNullable(queryParameters.get(param));
//    }

}
