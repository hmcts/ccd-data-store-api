package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserId {

    private String id;

    public UserId(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("UserId{id='%s'}", id);
    }
}
