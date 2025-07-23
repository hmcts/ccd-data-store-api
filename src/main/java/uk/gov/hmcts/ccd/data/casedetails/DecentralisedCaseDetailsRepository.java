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

    public Optional<CaseDetails> findFromShellCase(CaseDetails shellCase) {
        CaseDetails caseDetails = servicePersistenceAPI.getCase(shellCase.getReference());
        // Decentralised services don't have our private ID and it isn't part of the decentralised contract.
        // We set it here for internal use only.
        caseDetails.setId(shellCase.getId());
        return Optional.of(caseDetails);
    }
}
