package uk.gov.hmcts.ccd.domain.model.search.global;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@SuppressWarnings("unchecked")
public class SearchCriteria {

    private List<@Pattern(regexp = "^[\\d*?-]*$", message = ValidationError.CASE_REFERENCE_INVALID) String>
        caseReferences;

    @JsonProperty("CCDJurisdictionIds")
    private List<@Size(max = 70, message = ValidationError.JURISDICTION_ID_LENGTH_INVALID) String> ccdJurisdictionIds;

    @JsonProperty("CCDCaseTypeIds")
    private List<@Size(max = 70, message = ValidationError.CASE_TYPE_ID_LENGTH_INVALID) String> ccdCaseTypeIds;

    private List<@Size(max = 70, message = ValidationError.STATE_ID_LENGTH_INVALID) String> stateIds;

    private List<String> caseManagementRegionIds;

    private List<String> caseManagementBaseLocationIds;

    private List<String> otherReferences;

    @Valid
    private List<Party> parties;

    @JsonIgnore
    public Boolean getNonNullFields() throws IllegalAccessException {
        Field[] searchCriteriaFields = SearchCriteria.class.getDeclaredFields();
        List<Field> nonNullFields = new ArrayList<>();
        for (Field field : searchCriteriaFields) {
            if (field.get(this) != null) {
                ArrayList<Object> fieldListObjects = (ArrayList<Object>) field.get(this);
                if (field.getName().equals("parties")) {
                    for (Object listObject : fieldListObjects) {
                        Party party = (Party) listObject;
                        if (party.getNumberOfNonNullFields() > 0) {
                            return true;
                        }
                    }
                } else if (fieldListObjects.stream().anyMatch(Objects::nonNull)) {
                    nonNullFields.add(field);
                }
            }
        }
        return !nonNullFields.isEmpty();
    }
}


