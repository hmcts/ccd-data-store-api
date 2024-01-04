package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessTypeRolesDefinition implements Serializable, Copyable<AccessTypeRolesDefinition> {

    private String groupRoleName;
    private String caseAssignedRoleField;
    private String caseAccessGroupIdTemplate;

    @Override
    public AccessTypeRolesDefinition createCopy() {
        AccessTypeRolesDefinition copy = new AccessTypeRolesDefinition();
        copy.setGroupRoleName(this.getGroupRoleName());
        copy.setCaseAssignedRoleField(this.getCaseAssignedRoleField());
        copy.setCaseAccessGroupIdTemplate(this.getCaseAccessGroupIdTemplate());

        return copy;
    }
}
