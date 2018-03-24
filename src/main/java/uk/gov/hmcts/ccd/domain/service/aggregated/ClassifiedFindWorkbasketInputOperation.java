package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

@Service
@Qualifier(ClassifiedFindWorkbasketInputOperation.QUALIFIER)
public class ClassifiedFindWorkbasketInputOperation implements FindWorkbasketInputOperation {
    public static final String QUALIFIER = "classified";
    private final FindWorkbasketInputOperation findWorkbasketInputOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final SecurityClassificationService classificationService;

    public ClassifiedFindWorkbasketInputOperation(@Qualifier(DefaultFindWorkbasketInputOperation.QUALIFIER) final
                                                  FindWorkbasketInputOperation findWorkbasketInputOperation,
                                                  @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final
                                                  CaseDefinitionRepository caseDefinitionRepository,
                                                  SecurityClassificationService classificationService) {
        this.findWorkbasketInputOperation = findWorkbasketInputOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.classificationService = classificationService;
    }

    @Override
    public List<WorkbasketInput> execute(String jurisdictionId, String caseTypeId, Predicate<AccessControlList> access) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        return findWorkbasketInputOperation.execute(jurisdictionId, caseTypeId, access).stream()
            .filter(workbasketInput -> classificationService.userHasEnoughSecurityClassificationForField(jurisdictionId,
                caseType, workbasketInput.getField().getId())
            ).collect(toList());
    }
}
