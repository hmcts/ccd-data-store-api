package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

@Service
@Qualifier(DefaultGetCaseTypeOperation.QUALIFIER)
public class DefaultGetCaseTypeOperation implements GetCaseTypeOperation {
    public static final String QUALIFIER = "default";

    private final CaseTypeService caseTypeService;

    @Autowired
    public DefaultGetCaseTypeOperation(final CaseTypeService caseTypeService) {
        this.caseTypeService = caseTypeService;
    }

    @Override
    public Optional<CaseTypeDefinition> execute(String caseTypeId, Predicate<AccessControlList> access) {
        return Optional.ofNullable(caseTypeService.getCaseType(caseTypeId));
    }

}
