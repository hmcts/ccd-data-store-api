package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

import static uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation.INC;
import static uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation.SET;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataUpdateOperation implements SupplementaryDataUpdateOperation {

    private final SupplementaryDataRepository supplementaryDataRepository;

    private EnumMap<SupplementaryDataOperation, BiConsumer<String, SupplementaryDataUpdateRequest>> supplementaryFunctions =
        new EnumMap<>(SupplementaryDataOperation.class);

    @Autowired
    public DefaultSupplementaryDataUpdateOperation(final @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
        supplementaryFunctions.put(SET, (caseReference, updateRequest) -> {
            Map<String, Object> requestedProperties = updateRequest.getOperationProperties(SET);
            for (Map.Entry<String, Object> pathValuePair : requestedProperties.entrySet()) {
                this.supplementaryDataRepository.setSupplementaryData(caseReference, pathValuePair.getKey(), pathValuePair.getValue());
            }
        });
        supplementaryFunctions.put(INC, (caseReference, updateRequest) -> {
            Map<String, Object> requestedProperties = updateRequest.getOperationProperties(INC);
            for (Map.Entry<String, Object> pathValuePair : requestedProperties.entrySet()) {
                this.supplementaryDataRepository.incrementSupplementaryData(caseReference, pathValuePair.getKey(), pathValuePair.getValue());
            }
        });
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        supplementaryData.getOperations().forEach(operationID -> executeOperation(operationID, caseReference, supplementaryData));
        return this.supplementaryDataRepository.findSupplementaryData(caseReference, supplementaryData.getPropertiesNames());
    }

    private void executeOperation(String operationID, String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        Optional<SupplementaryDataOperation> operation = SupplementaryDataOperation.getOperation(operationID);
        operation.ifPresent(op ->
            supplementaryFunctions.get(operation.get()).accept(caseReference, supplementaryData
        ));
    }
}
