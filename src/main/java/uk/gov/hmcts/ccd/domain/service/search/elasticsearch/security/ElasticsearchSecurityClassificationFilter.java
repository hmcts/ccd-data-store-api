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
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

@Component
public class ElasticsearchSecurityClassificationFilter implements CaseSearchFilter {

    private final UserRepository userRepository;
    private final CaseTypeService caseTypeService;

    @Autowired
    public ElasticsearchSecurityClassificationFilter(@Qualifier(CachedUserRepository.QUALIFIER)
                                                             UserRepository userRepository,
                                                     CaseTypeService caseTypeService) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
    }

    @Override
    public Optional<QueryBuilder> getFilter(String caseTypeId) {
        return Optional.of(QueryBuilders.termsQuery(SECURITY_CLASSIFICATION_FIELD_COL,
            getSecurityClassifications(caseTypeId)));
    }

    private List<String> getSecurityClassifications(String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseTypeId);
        return userRepository.getHighestUserClassification(caseTypeDefinition.getJurisdictionDefinition().getId())
            .getClassificationsLowerOrEqualTo();
    }
}
