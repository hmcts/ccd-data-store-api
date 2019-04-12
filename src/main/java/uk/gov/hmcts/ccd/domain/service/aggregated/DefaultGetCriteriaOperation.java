package uk.gov.hmcts.ccd.domain.service.aggregated;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;

import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier(DefaultGetCriteriaOperation.QUALIFIER)
public class DefaultGetCriteriaOperation implements GetCriteriaOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGetCriteriaOperation.class);

    public static final String QUALIFIER = "default";
    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public DefaultGetCriteriaOperation(UIDefinitionRepository uiDefinitionRepository,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public <T> List<? extends CriteriaInput> execute(final String caseTypeId, final Predicate<AccessControlList> access, CriteriaType criteriaType) {
        LOG.debug("Finding WorkbasketInput fields for caseType={}", caseTypeId);

        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        List<CriteriaInput> criteriaInputs;
        if (criteriaType.equals(WORKBASKET)) {
            WorkbasketInputDefinition workbasketInputDefinition = uiDefinitionRepository.getWorkbasketInputDefinitions(caseTypeId);
            criteriaInputs = workbasketInputDefinition.getFields()
                .stream()
                .map(field -> toCriteriaInput(field, caseType, criteriaType))
                .collect(toList());
        } else {
            final SearchInputDefinition searchInputDefinition = uiDefinitionRepository.getSearchInputDefinitions(caseTypeId);
            criteriaInputs = searchInputDefinition.getFields()
                .stream()
                .map(field -> toCriteriaInput(field, caseType, criteriaType))
                .collect(toList());
        }
        return criteriaInputs;
    }
}
