package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

@Service
@Qualifier(ClassifiedFindSearchInputOperation.QUALIFIER)
public class ClassifiedFindSearchInputOperation implements FindSearchInputOperation {
    public static final String QUALIFIER = "classified";
    private final FindSearchInputOperation findSearchInputOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final SecurityClassificationService classificationService;

    public ClassifiedFindSearchInputOperation(@Qualifier(DefaultFindSearchInputOperation.QUALIFIER) final
                                                  FindSearchInputOperation findSearchInputOperation,
                                              @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final
                                                  CaseDefinitionRepository caseDefinitionRepository,
                                              SecurityClassificationService classificationService) {
        this.findSearchInputOperation = findSearchInputOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.classificationService = classificationService;
    }

    @Override
    public List<SearchInput> execute(String jurisdictionId, String caseTypeId, Predicate<AccessControlList> access) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        return findSearchInputOperation.execute(jurisdictionId, caseTypeId, access).stream()
            .filter(searchInput -> classificationService.userHasEnoughSecurityClassificationForField(jurisdictionId,
                caseType, searchInput.getField().getId())
            ).collect(toList());
    }
}
