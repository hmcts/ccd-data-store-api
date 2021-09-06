package uk.gov.hmcts.ccd.domain.service.globalsearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteriaResponse;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Component
@Slf4j
public class GlobalSearchParser {

    private static final String FIELD_SEPARATOR = ".";
    private final UserRepository userRepository;
    private final CaseTypeService caseTypeService;

    @Autowired
    public GlobalSearchParser(@Qualifier(CachedUserRepository.QUALIFIER)
                                  UserRepository userRepository,
                              CaseTypeService caseTypeService) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
    }

    public List<SearchCriteriaResponse> filterCases(List<SearchCriteriaResponse> results, SearchCriteria request) {
        List<String> fields = findFieldsToFilter(request);
        results.removeIf(searchCriteria -> !authorised(fields, searchCriteria));

        return results;
    }

    private boolean authorised(List<String> fields, SearchCriteriaResponse searchCriteria) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(searchCriteria.getCcdCaseTypeId());
        boolean condition = true;
        for (String field : fields) {
            Optional<CaseFieldDefinition> caseFieldDefinition =
                (field.contains(FIELD_SEPARATOR)) ? caseTypeDefinition.getComplexSubfieldDefinitionByPath(field)
                    : caseTypeDefinition.getCaseField(field);
            if (caseFieldDefinition.isPresent() && (!AccessControlService
                .hasAccessControlList(userRepository.getUserRoles(), CAN_READ,
                    caseFieldDefinition.get().getAccessControlLists())
                || caseFieldDefinition.get()
                .getSecurityLabel().equalsIgnoreCase(SecurityClassification.RESTRICTED.name()))) {
                condition = false;
                break;
            }

        }
        return condition;
    }

    private List<String> findFieldsToFilter(SearchCriteria request) {
        List<String> fields = new ArrayList<>();
        if (request.getCaseManagementBaseLocationIds() != null) {
            fields.add(SearchCriteriaResponse.SearchCriteriaEnum.BASE_LOCATION.getCcdField());
        }
        if (request.getCaseManagementRegionIds() != null) {
            fields.add(SearchCriteriaResponse.SearchCriteriaEnum.REGION.getCcdField());
        }
        if (request.getParties() != null) {
            fields.add(SearchCriteriaResponse.SearchCriteriaEnum.PARTIES.getCcdField());
        }
        return fields;
    }
}
