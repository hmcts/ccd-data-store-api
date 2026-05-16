package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.io.Serializable;

@Setter
@Getter
public class CaseStateLite implements Serializable, Copyable<CaseStateLite> {

    private String id = null;
    private String name = null;
    private String description = null;

    @JsonIgnore
    @Override
    public CaseStateLite createCopy() {
        CaseStateLite copy = new CaseStateLite();
        copy.setId(this.getId());
        copy.setName(this.getName());
        copy.setDescription(this.getDescription());
        return copy;
    }
}
