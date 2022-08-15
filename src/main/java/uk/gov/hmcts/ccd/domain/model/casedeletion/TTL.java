package uk.gov.hmcts.ccd.domain.model.casedeletion;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TTL {

    public static final String TTL_CASE_FIELD_ID = "TTL";
    public static final String YES = "Yes";
    public static final String NO = "No";

    private LocalDate systemTTL;

    private LocalDate overrideTTL;

    private String suspended;

    public boolean isSuspended() {
        return suspended != null && !suspended.equalsIgnoreCase(NO);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TTL ttl = (TTL) o;
        return Objects.equals(systemTTL, ttl.systemTTL)
            && Objects.equals(overrideTTL, ttl.overrideTTL)
            && suspendedValuesEqual(ttl.suspended);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            systemTTL,
            overrideTTL,
            suspended
        );
    }

    private boolean suspendedValuesEqual(String suspendedB) {
        // NB: not using comparison of `ttlA.isSuspended()` against `ttlB.isSuspended()` as
        //     null, 'No' and 'NO' all return the same isSuspended() = false,
        //     however we don't want to return true in this 'do values equal' when comparing `null = 'no'`.

        return Objects.equals(suspended, suspendedB)
            // NB: some callbacks may change case of YES/NO values: so use extra ignores case check
            || ((suspended != null) && suspended.equalsIgnoreCase(suspendedB));
    }

}
