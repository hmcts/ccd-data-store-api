package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;

@Component
@Slf4j
public class ElasticsearchUserCaseAccessFilter implements CaseSearchFilter {

    private final CaseAccessService caseAccessService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final ApplicationParams applicationParams;

    @Autowired
    public ElasticsearchUserCaseAccessFilter(CaseAccessService caseAccessService,
                                             @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                             final CaseDefinitionRepository caseDefinitionRepository,
                                             ApplicationParams applicationParams) {
        this.caseAccessService = caseAccessService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<QueryBuilder> getFilter(String caseTypeId) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return Optional.empty();
        }
        Instant start = Instant.now();
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            return Optional.empty();
        }

        return getGrantedCaseReferencesForRestrictedRoles(caseTypeDefinition).map(caseReferences -> {
            Duration between = Duration.between(start, Instant.now());
            log.info("retrieved {} granted case references {} in {} millisecs...",
                    caseReferences.size(), String.join(",", caseReferences.toString()), between.toMillis());
            return QueryBuilders.termsQuery(REFERENCE_FIELD_COL, caseReferences);
        });
    }

    private Optional<List<Long>> getGrantedCaseReferencesForRestrictedRoles(CaseTypeDefinition caseTypeDefinition) {
        return caseAccessService.getGrantedCaseReferencesForRestrictedRoles(caseTypeDefinition);
    }
}
