package uk.gov.hmcts.ccd.domain.service.aggregated;

import static java.util.stream.Collectors.toList;

import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier(ClassifiedGetCriteriaOperation.QUALIFIER)
public class ClassifiedGetCriteriaOperation implements GetCriteriaOperation {
    public static final String QUALIFIER = "classified";
    private final GetCriteriaOperation getCriteriaOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final SecurityClassificationService classificationService;

    public ClassifiedGetCriteriaOperation(
        @Qualifier(DefaultGetCriteriaOperation.QUALIFIER) final GetCriteriaOperation getCriteriaOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        SecurityClassificationService classificationService) {
        this.getCriteriaOperation = getCriteriaOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.classificationService = classificationService;
    }

    @Override
    public <T> List<? extends CriteriaInput> execute(String caseTypeId,
                                                     Predicate<AccessControlList> access,
                                                     CriteriaType criteriaType) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        return getCriteriaOperation.execute(caseTypeId, access, criteriaType).stream()
            .filter(workbasketInput -> classificationService.userHasEnoughSecurityClassificationForField(
                caseTypeDefinition.getJurisdictionId(), caseTypeDefinition, workbasketInput.getField().getId()))
            .collect(toList());
    }
}
