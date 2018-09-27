package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.AuthorisedCaseDataFilter;
import uk.gov.hmcts.ccd.domain.service.common.AuthorisedCaseDefinitionDataService;

@Service
@Qualifier(AuthorisedCaseDetailsSearchOperation.QUALIFIER)
public class AuthorisedCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

    public static final String QUALIFIER = "AuthorisedCaseDetailsSearchOperation";

    private final CaseDetailsSearchOperation caseDetailsSearchOperation;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    private final AuthorisedCaseDataFilter authorisedCaseDataFilter;

    public AuthorisedCaseDetailsSearchOperation(
        @Qualifier(ElasticsearchCaseDetailsSearchOperation.QUALIFIER) CaseDetailsSearchOperation caseDetailsSearchOperation,
        AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
        AuthorisedCaseDataFilter authorisedCaseDataFilter) {

        this.caseDetailsSearchOperation = caseDetailsSearchOperation;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.authorisedCaseDataFilter = authorisedCaseDataFilter;
    }

    @Override
    public CaseDetailsSearchResult execute(String caseTypeId, String query) {
        return authorisedCaseDefinitionDataService
            .getAuthorisedCaseType(caseTypeId, CAN_READ)
            .map(caseType -> search(caseType, query))
            .orElseGet(() -> new CaseDetailsSearchResult(emptyList(), 0L));
    }

    private CaseDetailsSearchResult search(CaseType caseType, String query) {
        CaseDetailsSearchResult result = caseDetailsSearchOperation.execute(caseType.getId(), query);
        // filter case fields based on user authority
        if (isNotEmpty(result.getCases())) {
            result.getCases().forEach(caseDetails -> authorisedCaseDataFilter.filterFields(caseType, caseDetails));
        }

        return result;
    }
}
