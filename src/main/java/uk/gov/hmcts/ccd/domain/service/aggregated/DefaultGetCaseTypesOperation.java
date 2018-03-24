package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.List;
import java.util.function.Predicate;

@Service
@Qualifier(DefaultGetCaseTypesOperation.QUALIFIER)
public class DefaultGetCaseTypesOperation implements GetCaseTypesOperation {
    public static final String QUALIFIER = "default";

    private final CaseTypeService caseTypeService;

    @Autowired
    public DefaultGetCaseTypesOperation(final CaseTypeService caseTypeService) {
        this.caseTypeService = caseTypeService;
    }

    @Override
    public List<CaseType> execute(String jurisdictionId, Predicate<AccessControlList> access) {
        return caseTypeService.getCaseTypesForJurisdiction(jurisdictionId);
    }

}
