package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

@Component
public class ElasticsearchSecurityClassificationFilter implements CaseSearchFilter {

    private final UserRepository userRepository;
    private final CaseTypeService caseTypeService;
    private final ApplicationParams applicationParams;

    @Autowired
    public ElasticsearchSecurityClassificationFilter(@Qualifier(CachedUserRepository.QUALIFIER)
                                                             UserRepository userRepository,
                                                     CaseTypeService caseTypeService,
                                                     ApplicationParams applicationParams) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<Query> getFilter(String caseTypeId) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return Optional.empty();
        }

        List<String> classifications = getSecurityClassifications(caseTypeId);

        return Optional.of(Query.of(q -> q
            .terms(t -> t
                .field(SECURITY_CLASSIFICATION_FIELD_COL)
                .terms(tf -> tf.value(
                    classifications.stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList())
                ))
            )
        ));
    }

    private List<String> getSecurityClassifications(String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseTypeId);
        return userRepository.getHighestUserClassification(caseTypeDefinition.getJurisdictionDefinition().getId())
            .getClassificationsLowerOrEqualTo();
    }
}
