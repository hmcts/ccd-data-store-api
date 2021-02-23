package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentAttributesResource implements Serializable {
    private static final long serialVersionUID = -7106266789404292869L;

    Optional<String> jurisdiction;
    Optional<String> caseId;
    Optional<String> region;
    Optional<String> location;
    Optional<String> contractType;
}
