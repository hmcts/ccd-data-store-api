package uk.gov.hmcts.ccd.data.casedetails;

import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;

@Service
public class POCCaseAuditEventRepository {


    private final PocApiClient pocApiClient;
    private final SecurityUtils securityUtils;
    private final RoleAssignmentService roleAssignmentService;


    @Inject
    public POCCaseAuditEventRepository(final PocApiClient pocApiClient,
                                       final SecurityUtils securityUtils,
                                       final RoleAssignmentService roleAssignmentService) {
        this.pocApiClient = pocApiClient;
        this.securityUtils = securityUtils;
        this.roleAssignmentService = roleAssignmentService;
    }

    public List<AuditEvent> findByCase(final CaseDetails caseDetails) {

        Long reference = caseDetails.getReference();
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        return pocApiClient.getEvents(reference.toString(), roleAssignments);
    }

}
