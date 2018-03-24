package uk.gov.hmcts.ccd.domain.model.aggregated;

public class IDAMProperties {
    private String id;
    private String email;
    private String forename;
    private String surname;
    private String[] roles;
    private String defaultService;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getForename() {
        return forename;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public String getDefaultService() {
        return defaultService;
    }

    public void setDefaultService(String defaultService) {
        this.defaultService = defaultService;
    }
}
