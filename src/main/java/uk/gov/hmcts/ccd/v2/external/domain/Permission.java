package uk.gov.hmcts.ccd.v2.external.domain;

/**
 * Exposes a set of enum values used to set permissions for Access Management.
 * Each of the values is a power of two. The reason for that is that in Access Management
 */

public enum Permission {
    CREATE(1),
    READ(2),
    UPDATE(4),
    DELETE(8);

    private int value;

    Permission(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
