package uk.gov.hmcts.ccd.data.casedetails;

import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.ServicePersistenceAPI;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Service
public class POCCaseAuditEventRepository {


    private final ServicePersistenceAPI servicePersistenceAPI;

    @Inject
    public POCCaseAuditEventRepository(final ServicePersistenceAPI servicePersistenceAPI) {
        this.servicePersistenceAPI = servicePersistenceAPI;
    }

    public List<AuditEvent> findByCase(final CaseDetails caseDetails) {

        Long reference = caseDetails.getReference();
        return servicePersistenceAPI.getCaseHistory(reference.toString());
    }

}
