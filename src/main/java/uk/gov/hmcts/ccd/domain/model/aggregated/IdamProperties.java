package uk.gov.hmcts.ccd.domain.model.aggregated;

public class IdamProperties extends IdamUser {

    private static final long serialVersionUID = -8859850331834643245L;
    private String[] roles;
    private String defaultService;

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
