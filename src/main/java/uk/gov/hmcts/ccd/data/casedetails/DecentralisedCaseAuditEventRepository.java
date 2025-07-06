package uk.gov.hmcts.ccd.data.casedetails;

import java.util.List;
import javax.inject.Inject;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.ServicePersistenceAPI;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

@RequiredArgsConstructor
@Service
public class DecentralisedCaseAuditEventRepository {


    private final ServicePersistenceAPI servicePersistenceAPI;
    private final PersistenceStrategyResolver persistenceStrategyResolver;

    public List<AuditEvent> findByCase(final CaseDetails caseDetails) {

        Long reference = caseDetails.getReference();
        var uri = persistenceStrategyResolver.resolveUriOrThrow(caseDetails);
        return servicePersistenceAPI.getCaseHistory(uri, reference.toString());
    }
}
