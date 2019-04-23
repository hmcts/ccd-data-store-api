package uk.gov.hmcts.ccd.domain.model.aggregated;

import java.io.Serializable;

public class IdamUser implements Serializable {
    private static final long serialVersionUID = -1598749846026580369L;
    private String id;
    private String email;
    private String forename;
    private String surname;

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
}
