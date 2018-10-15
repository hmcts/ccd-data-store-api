package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

@Component
public class ElasticsearchSecurityClassificationFilter implements CaseSearchFilter {

    private final UserRepository userRepository;

    @Autowired
    public ElasticsearchSecurityClassificationFilter(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<QueryBuilder> getFilter(String caseTypeId) {
        return Optional.of(QueryBuilders.termsQuery(SECURITY_CLASSIFICATION_FIELD_COL, getSecurityClassifications()));
    }

    private List<String> getSecurityClassifications() {
        return userRepository.getHighestUserClassification().getClassificationsLowerOrEqualTo();
    }
}
