package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CaseAccessGroups {

    public static final String CASE_ACCESS_GROUPS_FIELD_ID = "CaseAccessGroups";

    List<CaseAccessGroup> caseAccessGroups;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaseAccessGroups caseAccessGroupsList = (CaseAccessGroups) o;
        return Objects.equals(caseAccessGroups, caseAccessGroupsList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            caseAccessGroups
        );
    }

}
