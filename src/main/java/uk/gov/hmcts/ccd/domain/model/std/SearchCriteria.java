package uk.gov.hmcts.ccd.domain.model.std;

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

    @Pattern(regexp = "^[\\d*-?]*$", message = ValidationError.CASE_REFERENCE_INVALID)
    public String caseReferences;

    @JsonProperty("CCDJurisdictionIds")
    @Size(max = 70, message = ValidationError.JURISDICTION_ID_LENGTH_INVALID)
    public String ccdJurisdictionIds;

    @JsonProperty("CCDCaseTypeIds")
    @Size(max = 70, message = ValidationError.CASE_TYPE_ID_LENGTH_INVALID)
    public String ccdCaseTypeIds;

    @Size(max = 70, message = ValidationError.STATE_ID_LENGTH_INVALID)
    public String stateIds;

    public String caseManagementRegionIds;

    public String caseManagementBaseLocationIds;

    public String otherReferences;

    @ValidPartiesItems
    @Valid
    @JsonProperty("Parties")
    public List<Parties> parties;
}
