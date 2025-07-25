package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

@Service
@Qualifier("default")
@RequiredArgsConstructor
public class DelegatingSupplementaryDataUpdateOperation implements SupplementaryDataUpdateOperation {

    private final PersistenceStrategyResolver persistenceResolver;
    private final DefaultSupplementaryDataUpdateOperation defaultSupplementaryDataUpdateOperation;
    private final ServicePersistenceClient servicePersistenceClient;

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference,
                                                     SupplementaryDataUpdateRequest supplementaryData) {
        if (persistenceResolver.isDecentralised(Long.valueOf(caseReference))) {
            var updated = servicePersistenceClient.updateSupplementaryData(
                Long.valueOf(caseReference),
                supplementaryData
            );
            return new SupplementaryData(updated, supplementaryData.getPropertiesNames());
        }

        return defaultSupplementaryDataUpdateOperation.updateSupplementaryData(caseReference, supplementaryData);
    }
}
