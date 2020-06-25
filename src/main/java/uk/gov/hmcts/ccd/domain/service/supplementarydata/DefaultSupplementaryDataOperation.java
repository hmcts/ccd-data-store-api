package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.SupplementaryDataProcessor;
import uk.gov.hmcts.ccd.data.casedetails.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataRequest;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataOperation implements SupplementaryDataOperation {

    private final SupplementaryDataRepository supplementaryDataRepository;

    private final SupplementaryDataProcessor supplementaryDataProcessor = new SupplementaryDataProcessor();

    @Autowired
    public DefaultSupplementaryDataOperation(final @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataRequest supplementaryData) {
        incrementData(caseReference, supplementaryData);
        setData(caseReference, supplementaryData);
        return this.supplementaryDataRepository.findSupplementaryData(caseReference);
    }

    private void incrementData(String caseReference, SupplementaryDataRequest supplementaryData) {
        Map<String, Object> incrementRequest = supplementaryDataProcessor
            .accessLeafNodes(supplementaryData.getRequestData().get(Operation.INC.getOperationName()));
        if (incrementRequest.size() > 0) {
            this.supplementaryDataRepository.incrementSupplementaryData(caseReference, incrementRequest);
        }
    }

    private void setData(String caseReference, SupplementaryDataRequest supplementaryData) {
        Map<String, Object> setRequest = supplementaryDataProcessor
            .accessLeafNodes(supplementaryData.getRequestData().get(Operation.SET.getOperationName()));
        if (setRequest.size() > 0) {
            this.supplementaryDataRepository.setSupplementaryData(caseReference, setRequest);
        }
    }
}
