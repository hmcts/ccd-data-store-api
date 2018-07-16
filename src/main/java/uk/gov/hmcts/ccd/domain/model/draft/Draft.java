package uk.gov.hmcts.ccd.domain.model.draft;

import lombok.ToString;

@ToString
public class Draft {

    private Long id;

    public Draft() {
    /*
        Jackson required
     */
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
