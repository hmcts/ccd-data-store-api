package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Strings;

import java.util.Arrays;

public enum UseCase {

    WORKBASKET("WORKBASKET"),
    SEARCH("SEARCH"),
    ORG_CASES("ORGCASES"),
    DEFAULT("");

    private String reference;

    UseCase(String reference) {
        this.reference = reference;
    }

    @JsonValue
    public String getReference() {
        return reference;
    }

    @JsonCreator
    public static UseCase valueOfReference(String reference) {
        if (Strings.isNullOrEmpty(reference)) {
            return DEFAULT;
        }
        return Arrays.stream(UseCase.values())
            .filter(useCase -> useCase.getReference().equals(reference))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Use case reference '%s' is not a known value.", reference)));
    }
}
