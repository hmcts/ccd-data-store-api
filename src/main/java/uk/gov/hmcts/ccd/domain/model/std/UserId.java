package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class UserId {

    private String id;

    public UserId(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
