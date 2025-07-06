package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.ServicePersistenceAPI;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

@Service
@Qualifier("default")
public class DelegatingSupplementaryDataUpdateOperation implements SupplementaryDataUpdateOperation {

    private final CaseDetailsRepository caseDetailsRepository;
    private final PersistenceStrategyResolver persistenceResolver;
    private final DefaultSupplementaryDataUpdateOperation defaultSupplementaryDataUpdateOperation;
    private final ServicePersistenceAPI servicePersistenceAPI;

    @Autowired
    public DelegatingSupplementaryDataUpdateOperation(
        @Qualifier("default") CaseDetailsRepository caseDetailsRepository,
        PersistenceStrategyResolver persistenceResolver,
        @Qualifier("db") DefaultSupplementaryDataUpdateOperation defaultSupplementaryDataUpdateOperation,
        ServicePersistenceAPI servicePersistenceAPI) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.persistenceResolver = persistenceResolver;
        this.defaultSupplementaryDataUpdateOperation = defaultSupplementaryDataUpdateOperation;
        this.servicePersistenceAPI = servicePersistenceAPI;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        Optional<URI> serviceUrl = persistenceResolver.resolveUrl(caseReference);

        if (serviceUrl.isPresent()) {
            return servicePersistenceAPI.updateSupplementaryData(
                serviceUrl.get(),
                caseReference,
                supplementaryData
            );
        }

        return defaultSupplementaryDataUpdateOperation.updateSupplementaryData(caseReference, supplementaryData);
    }
}
