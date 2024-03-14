package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonRootName(value = "value")
public class CaseAccessGroup {

    public static final String CASE_ACCESS_GROUP_FIELD_ID = "caseAccessGroup";

    private String caseAccessGroupType;
    private String caseAccessGroupId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaseAccessGroup caseAccessGroup = (CaseAccessGroup) o;
        return Objects.equals(caseAccessGroupType, caseAccessGroup.caseAccessGroupType)
            && Objects.equals(caseAccessGroupId, caseAccessGroup.caseAccessGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            caseAccessGroupType,
            caseAccessGroupId
        );
    }

}
