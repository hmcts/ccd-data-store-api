package uk.gov.hmcts.ccd.domain.model.search.global;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class SearchCriteriaResponse {

    private @Pattern(regexp = "^[\\d*?-]*$", message = ValidationError.CASE_REFERENCE_INVALID) String
        caseReferences;

    private @Size(max = 70, message = ValidationError.JURISDICTION_ID_LENGTH_INVALID) String ccdJurisdictionId;

    private @Size(max = 70, message = ValidationError.CASE_TYPE_ID_LENGTH_INVALID) String ccdCaseTypeId;

    private @Size(max = 70, message = ValidationError.STATE_ID_LENGTH_INVALID) String stateId;

    private String caseManagementRegionId;

    private String caseManagementBaseLocationId;

    private String hmctsServiceId;

    private String caseNameHmctsInternal;

    private String caseManagementCategoryName;

    private List<String> otherReferences;

    private List<Party> parties;

    public enum SearchCriteriaEnum {
        REGION("caseManagementRegionId", "caseManagementLocation.region"),
        BASE_LOCATION("caseManagementBaseLocationId", "caseManagementLocation.baseLocation"),
        PARTIES("parties", "SearchCriteria.SearchParties"),
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
