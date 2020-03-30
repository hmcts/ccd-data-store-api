package uk.gov.hmcts.ccd.v2.external.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Builder
public class CaseDocument {
    private String url;
    private String name;
    private String type;
    private String description;
    private String id;
    private List<Permission> permissions;
    @JsonIgnore
    private String hashedToken;
}
