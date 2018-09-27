package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

@Component
public class DefaultAuthorisedCaseDataFilter implements AuthorisedCaseDataFilter {

    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final SecurityClassificationService classificationService;
    private final ObjectMapperService objectMapperService;

    @Autowired
    public DefaultAuthorisedCaseDataFilter(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                           AccessControlService accessControlService,
                                           SecurityClassificationService classificationService,
                                           ObjectMapperService objectMapperService) {
        this.userRepository = userRepository;
        this.accessControlService = accessControlService;
        this.classificationService = classificationService;
        this.objectMapperService = objectMapperService;
    }

    @Override
    public void filterFields(CaseType caseType, CaseDetails caseDetails) {
        filterCaseFieldsByAccess(caseType, caseDetails);
        classificationService.applyClassification(caseDetails);
    }

    private void filterCaseFieldsByAccess(CaseType caseType, CaseDetails caseDetails) {
        JsonNode data = objectMapperService.convertObjectToJsonNode(caseDetails.getData());
        JsonNode filteredData = accessControlService.filterCaseFieldsByAccess(data, caseType.getCaseFields(), getUserRoles(), AccessControlService.CAN_READ);
        caseDetails.setData(objectMapperService.convertJsonNodeToMap(filteredData));
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }
}
