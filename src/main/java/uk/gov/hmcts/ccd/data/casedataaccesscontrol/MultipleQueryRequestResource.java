package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Getter
@Jacksonized
public class MultipleQueryRequestResource {

    private final List<RoleAssignmentQuery> queryRequests;

}
