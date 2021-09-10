package uk.gov.hmcts.ccd.domain.model.search.global;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchCriteriaResponse {

    private String caseReference;

    private String jurisdictionId;

    private String caseTypeId;

    private String stateId;

    private String region;

    private String baseLocation;

    private String HmctsServiceId;

    private String caseNameHmctsInternal;

    private String caseManagementCategoryName;

    private List<String> otherCaseReferences;

    private List<Party> searchParties;

    public enum SearchCriteriaEnum {
        REGION("region", "caseManagementLocation.region"),
        BASE_LOCATION("baseLocation", "caseManagementLocation.baseLocation"),
        PARTIES("SearchParties", "SearchCriteria.SearchParties"),
        OTHER_CASE_REFERENCES("otherCaseReferences", "SearchCriteria.OtherCaseReferences");

        private final String searchCriteriaField;
        private final String ccdField;

        SearchCriteriaEnum(String searchCriteriaField, String ccdField) {
            this.searchCriteriaField = searchCriteriaField;
            this.ccdField = ccdField;
        }

        public String getSearchCriteriaField() {
            return searchCriteriaField;
        }

        public String getCcdField() {
            return ccdField;
        }
    }

}
