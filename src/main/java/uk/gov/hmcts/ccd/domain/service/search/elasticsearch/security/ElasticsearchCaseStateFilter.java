package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

@Component
public class ElasticsearchCaseStateFilter implements CaseSearchFilter {

    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    private final ApplicationParams applicationParams;

    @Autowired
    public ElasticsearchCaseStateFilter(
        AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
        ApplicationParams applicationParams) {
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<Query> getFilter(String caseTypeId) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return Optional.empty();
        }

        List<String> caseStates = getCaseStateIdsForUserReadAccess(caseTypeId);

        return Optional.of(Query.of(q -> q
            .terms(t -> t
                .field(STATE_FIELD_COL)
                .terms(tf -> tf.value(
                    caseStates.stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList())
                ))
            )
        ));
    }

    private List<String> getCaseStateIdsForUserReadAccess(String caseTypeId) {
        return authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ)
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }
}
