package uk.gov.hmcts.ccd.v2.external.domain;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class CaseUser {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("case_roles")
    private Set<String> caseRoles = new HashSet<>();
}
