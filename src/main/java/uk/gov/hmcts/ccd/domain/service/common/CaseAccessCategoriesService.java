package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class CaseAccessCategoriesService {

    public Predicate<CaseDetails> caseHasMatchingCaseAccessCategories(
        Set<AccessProfile> accessProfiles, boolean create) {
        return cd -> {
            if (create || hasEmptyCaseAccessCategory(accessProfiles)) {
                return true;
            }
            String value = getCaseAccessCategory(cd);
            Set<String> caseAccessCategories = getCaseAccessCategories(accessProfiles);
            return !StringUtils.isEmpty(value) && caseAccessCategories.stream()
                .anyMatch(value::startsWith);
        };
    }

    private String getCaseAccessCategory(CaseDetails cd) {
        JsonNode caseAccessCategory = null;
        if (cd.getData() != null) {
            caseAccessCategory = cd.getData().get("CaseAccessCategory");
        }
        return caseAccessCategory != null ? caseAccessCategory.asText() : "";
    }

    private Set<String> getCaseAccessCategories(Set<AccessProfile> accessProfiles) {
        return accessProfiles.stream()
            .filter(ap -> ap.getCaseAccessCategories() != null)
            .flatMap(ap -> Arrays.stream(ap.getCaseAccessCategories().split(",")))
            .collect(Collectors.toSet());
    }

    private boolean hasEmptyCaseAccessCategory(Set<AccessProfile> accessProfiles) {
        return accessProfiles.stream()
            .anyMatch(ap -> StringUtils.isEmpty(ap.getCaseAccessCategories()));
    }
}
