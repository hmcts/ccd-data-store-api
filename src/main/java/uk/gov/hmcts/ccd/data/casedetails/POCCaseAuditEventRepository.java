package uk.gov.hmcts.ccd.data.casedetails;

import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Service
public class POCCaseAuditEventRepository {


    private final PocApiClient pocApiClient;


    @Inject
    public POCCaseAuditEventRepository(final PocApiClient pocApiClient) {
        this.pocApiClient = pocApiClient;
    }

    public List<AuditEvent> findByCase(final CaseDetails caseDetails) {

        Long reference = caseDetails.getReference();
        return pocApiClient.getEvents(reference.toString());
    }

}
