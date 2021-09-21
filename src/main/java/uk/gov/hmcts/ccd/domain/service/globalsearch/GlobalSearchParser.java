package uk.gov.hmcts.ccd.domain.service.globalsearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

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
    private final SecurityClassificationService securityClassificationService;

    @Autowired
    public GlobalSearchParser(@Qualifier(CachedUserRepository.QUALIFIER)
                                  UserRepository userRepository,
                              CaseTypeService caseTypeService,
                              SecurityClassificationService securityClassificationService) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
        this.securityClassificationService = securityClassificationService;
    }

    public List<CaseDetails> filterCases(List<CaseDetails> results, SearchCriteria request) {
        List<String> fields = findFieldsToFilter(request);
        results.removeIf(caseDetails -> !authorised(fields, caseDetails));
        return results;
    }

    private boolean authorised(List<String> fields, CaseDetails caseDetails) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseDetails.getCaseTypeId());
        boolean isAuthorised = true;
        for (String field : fields) {
            Optional<CaseFieldDefinition> caseFieldDefinition =
                (field.contains(FIELD_SEPARATOR)) ? caseTypeDefinition.getComplexSubfieldDefinitionByPath(field)
                    : caseTypeDefinition.getCaseField(field);
            if (caseFieldDefinition.isPresent() && ((!AccessControlService
                .hasAccessControlList(userRepository.getUserRoles(), CAN_READ,
                    caseFieldDefinition.get().getAccessControlLists()))
                || !securityClassificationService
                .userHasEnoughSecurityClassificationForField(caseTypeDefinition.getJurisdictionId(),
                    SecurityClassification.valueOf(caseFieldDefinition.get().getSecurityLabel())))) {
                isAuthorised = false;
                break;
            }
        }
        return isAuthorised;
    }

    private List<String> findFieldsToFilter(SearchCriteria request) {
        List<String> fields = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getCaseManagementBaseLocationIds())) {
            fields.add(SearchCriteria.SearchCriteriaEnum.BASE_LOCATION.getCcdField());
        }
        if (!CollectionUtils.isEmpty(request.getCaseManagementRegionIds())) {
            fields.add(SearchCriteria.SearchCriteriaEnum.REGION.getCcdField());
        }
        if (!CollectionUtils.isEmpty(request.getParties())) {
            fields.add(SearchCriteria.SearchCriteriaEnum.PARTIES.getCcdField());
        }
        if (!CollectionUtils.isEmpty(request.getOtherReferences())) {
            fields.add(SearchCriteria.SearchCriteriaEnum.OTHER_CASE_REFERENCES.getCcdField());
        }
        return fields;
    }
}
