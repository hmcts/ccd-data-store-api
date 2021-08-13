package uk.gov.hmcts.ccd.domain.model.search.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidPartiesItems;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class SearchCriteria {

    private List<@Pattern(regexp = "^[\\d*-?]*$", message = ValidationError.CASE_REFERENCE_INVALID) String>
        caseReferences;

    @JsonProperty("CCDJurisdictionIds")
    private List<@Size(max = 70, message = ValidationError.JURISDICTION_ID_LENGTH_INVALID) String> ccdJurisdictionIds;

    @JsonProperty("CCDCaseTypeIds")
    private List<@Size(max = 70, message = ValidationError.CASE_TYPE_ID_LENGTH_INVALID) String> ccdCaseTypeIds;

    private List<@Size(max = 70, message = ValidationError.STATE_ID_LENGTH_INVALID) String> stateIds;

    private List<String> caseManagementRegionIds;

    private List<String> caseManagementBaseLocationIds;

    private List<String> otherReferences;

    @ValidPartiesItems
    @Valid
    @JsonProperty("Parties")
    private List<Party> parties;
}
