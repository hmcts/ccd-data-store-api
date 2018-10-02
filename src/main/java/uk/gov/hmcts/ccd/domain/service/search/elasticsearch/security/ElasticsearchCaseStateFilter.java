package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

@Component
public class ElasticsearchCaseStateFilter implements CaseSearchFilter {

    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @Autowired
    public ElasticsearchCaseStateFilter(
        AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService) {
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
    }

    @Override
    public Optional<QueryBuilder> getFilter(String caseTypeId) {
        return Optional.of(QueryBuilders.termsQuery(STATE_FIELD_COL, getCaseStateIdsForUserReadAccess(caseTypeId)));
    }

    private List<String> getCaseStateIdsForUserReadAccess(String caseTypeId) {
        return authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ)
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }
}
