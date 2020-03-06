package uk.gov.hmcts.ccd.v2.external.domain;

/**
 * Exposes a set of enum values used to set permissions for Access Management.
 * Each of the values is a power of two. The reason for that is that in Access Management
 * there might be multiple permissions that need to be assigned: ie. READ + CREATE.
 * In order to determine which individual permissions a record has
 * the binary 'AND' operation is done (the 'isGranted' method).
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

    /**
     * Performs a binary AND operation to determine whether permission can be derived from the sum of permissions.
     *
     * @param permissions the numeric sum of permissions
     * @return true if particular permission is included is sum of permissions, otherwise false
     */
    public boolean isGranted(int permissions) {
        return (permissions & this.getValue()) == this.getValue();
    }
}
