package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.Copyable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
public class CaseEventLite implements Serializable, Copyable<CaseEventLite> {

    private String id = null;
    private String name = null;
    private String description = null;
    @JsonProperty("pre_states")
    private List<String> preStates = new ArrayList<>();
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;

    @JsonIgnore
    @Override
    public CaseEventLite createCopy() {
        CaseEventLite copy = new CaseEventLite();
        copy.setId(this.getId());
        copy.setName(this.getName());
        copy.setDescription(this.getDescription());
        copy.setPreStates(this.getPreStates() != null ? new ArrayList<>(this.getPreStates()) : null);
        copy.setAccessControlLists(createACLCopyList(this.getAccessControlLists()));
        return copy;
    }
}
