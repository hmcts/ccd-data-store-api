package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class RoleAssignmentsDeleteRequest {

    String caseId;

    String userId;

    List<String> roleNames;

}
