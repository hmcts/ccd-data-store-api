package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Used Optional<Striing> in this class, this will help to have three different values for the object when
 * JSON de-serialisation is invoked.
 *
 * eeg:
 * When JSON don't have the jurisdiction field, then value of Optional<String> jurisdiction = null
 *
 * When JSON have the jurisdiction field, and the value of that field is null,
 * then value of Optional<String> jurisdiction = Optional.empty
 *
 * When JSON have the jurisdiction field, and the value of that field is XYZ,
 * then value of Optional<String> jurisdiction = "XYZ"
 *
 */
public class RoleAssignmentAttributesResource implements Serializable {
    private static final long serialVersionUID = -7106266789404292869L;

    Optional<String> jurisdiction;
    Optional<String> caseType;
    Optional<String> caseId;
    Optional<String> region;
    Optional<String> location;
    Optional<String> contractType;
    transient Optional<String> caseAccessGroupId;

}
