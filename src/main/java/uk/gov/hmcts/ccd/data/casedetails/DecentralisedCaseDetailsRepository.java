package uk.gov.hmcts.ccd.data.casedetails;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Service
@Slf4j
public class DecentralisedCaseDetailsRepository {

    private final ServicePersistenceClient servicePersistenceAPI;

    public DecentralisedCaseDetailsRepository(final ServicePersistenceClient servicePersistenceAPI) {
        this.servicePersistenceAPI = servicePersistenceAPI;
    }

    public Optional<CaseDetails> findByReference(Long reference) {
        return Optional.of(getCaseDetails(reference));
    }


    private CaseDetails getCaseDetails(Long reference) {
        CaseDetails caseDetails = servicePersistenceAPI.getCase(reference);
        // TODO: Remove this when legacy RBAC code is removed which relies on details having an id.
        caseDetails.setId(caseDetails.getReference().toString());
        return caseDetails;
    }
}
