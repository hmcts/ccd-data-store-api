package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

import java.util.List;

@Data
@EqualsAndHashCode
@Builder
public class CaseDocument {
    private String url;
    private String name;
    private String type;
    private String description;
    private String id;
    private List<AccessControlList> accessControlLists;
    @JsonIgnore
    private String hashedToken;
}
