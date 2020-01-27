package uk.gov.hmcts.ccd.domain.model.common;

public enum CatalogueResponseGroup {

    SUCCESS("CCD", 0),
    VALIDATION("CCD", 1),
    CALLBACK("CCD", 2);

    final String domain;
    final int groupId;

    CatalogueResponseGroup(final String domain, final int groupId) {
        this.domain = domain;
        this.groupId = groupId;
    }

    public String getDomain() {
        return domain;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getCode() {
        return String.format("%s.%02d", getDomain(), getGroupId());
    }

}
