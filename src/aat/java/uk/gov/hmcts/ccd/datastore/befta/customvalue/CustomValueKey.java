package uk.gov.hmcts.ccd.datastore.befta.customvalue;

import java.util.Arrays;

public enum CustomValueKey {

    // NB: this is the order that the `getEnum()` match statement will run
    CASE_ID_AS_INTEGER("caseIdAsIntegerFrom", MatchKey.startsWith),
    CASE_ID_AS_STRING("caseIdAsStringFrom", MatchKey.startsWith),
    HYPHENISED_CASE_ID_FROM_CASE_CREATION("HyphenisedCaseIdFromCaseCreation", MatchKey.startsWith),
    ORGANISATIONS_ASSIGNED_USERS("orgsAssignedUsers", MatchKey.startsWith),
    UNIQUE_STRING("UniqueString", MatchKey.equals),
    APPROXIMATELY("approximately ", MatchKey.startsWith),
    CONTAINS("contains ", MatchKey.startsWith),
    DOCUMENT_ID_IN_RESPONSE("documentIdInTheResponse", MatchKey.equals),
    VALID_SELF_LINK("validSelfLink", MatchKey.equalsIgnoreCase),
    VALID_BINARY_LINK("validBinaryLink", MatchKey.equalsIgnoreCase),

    DATE_TWENTY_DAYS_FROM_TODAY("dateTwentyDaysFromToday", MatchKey.equalsIgnoreCase),
    DATE_THIRTY_DAYS_FROM_TODAY("dateThirtyDaysFromToday", MatchKey.equalsIgnoreCase),
    DATE_GREATER_THAN_TTL_GUARD_DATE("dateGreaterThanTTLGuardDate", MatchKey.equalsIgnoreCase),
    DATE_LESS_THAN_TTL_GUARD_DATE("dateLessThanTTLGuardDate", MatchKey.equalsIgnoreCase),

    GENERATE_UUID("generateUUID", MatchKey.equalsIgnoreCase),
    DEFAULT_KEY("DefaultKey", MatchKey.equals);

    private final String value;
    private final MatchKey matchKey;

    CustomValueKey(final String value, final MatchKey matchKey) {
        this.value = value;
        this.matchKey = matchKey;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static CustomValueKey getEnum(final String value) {
        return Arrays.stream(values())
            .filter(key -> key.matches(value))
            .findFirst()
            .orElse(DEFAULT_KEY);
    }

    private boolean matches(final String value) {
        switch (this.matchKey) {
            case startsWith:
                return value.startsWith(this.value);

            case equalsIgnoreCase:
                return value.equalsIgnoreCase(this.value);

            default:
                return value.equals(this.value);
        }
    }

    private enum MatchKey {
        startsWith,
        equalsIgnoreCase,
        equals
    }

}
