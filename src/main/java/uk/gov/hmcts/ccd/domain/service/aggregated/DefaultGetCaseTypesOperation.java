package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

/**
 * Default {@link GetCaseTypesOperation}.
 *
 * @deprecated current implementation has serious performance issues
 */
@Deprecated
@Service
@SuppressWarnings("squid:S1133")
@Qualifier(DefaultGetCaseTypesOperation.QUALIFIER)
public class DefaultGetCaseTypesOperation implements GetCaseTypesOperation {
    public static final String QUALIFIER = "default";

    private final CaseTypeService caseTypeService;

    @Autowired
    public DefaultGetCaseTypesOperation(final CaseTypeService caseTypeService) {
        this.caseTypeService = caseTypeService;
    }

    @Override
    public List<CaseTypeDefinition> execute(String jurisdictionId, Predicate<AccessControlList> access) {
        return caseTypeService.getCaseTypesForJurisdiction(jurisdictionId);
    }

}
