package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleAssignmentQuery {

    private List<String> actorId;
    private List<String> roleType;
    private List<String> roleName;
    private List<String> classification;
    private List<String> grantType;
    private List<String> roleCategory;
    private List<String> validAt;
    private List<String> authorisations;
    private List<Attributes> attributes;
}
