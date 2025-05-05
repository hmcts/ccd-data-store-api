package uk.gov.hmcts.ccd.migration;

public class UserOrganisation {

    String organisationId;
    String userId;

    public UserOrganisation(String organisationId, String userId) {
        this.organisationId = organisationId;
        this.userId = userId;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UserOrganisation{" +
            "organisationId='" + organisationId + '\'' +
            ", userId='" + userId + '\'' +
            '}';
    }
}
