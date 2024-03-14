package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CaseAccessGroup {

    public static final String CASE_ACCESS_GROUP_FIELD_ID = "caseAccessGroup";

    private String caseAccessGroupId;
    private String caseAccessGroupType;

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
