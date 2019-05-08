package uk.gov.hmcts.ccd.data.casedetails.search;

import java.util.Optional;

public enum SortDirection {
    ASC, DESC;

    public static SortDirection fromOptionalString(Optional<String> direction) {
        if ("DESC".equalsIgnoreCase(direction.orElse(null))) {
            return DESC;
        }
        return ASC;
    }
}
