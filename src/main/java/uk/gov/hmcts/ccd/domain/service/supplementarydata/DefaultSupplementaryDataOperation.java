package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.Operation;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataOperation implements SupplementaryDataOperation {

    private final SupplementaryDataRepository supplementaryDataRepository;

    @Autowired
    public DefaultSupplementaryDataOperation(final @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        incrementData(caseReference, supplementaryData);
        setData(caseReference, supplementaryData);
        return this.supplementaryDataRepository.findSupplementaryData(caseReference);
    }

    private void incrementData(String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        Map<String, Object> incrementRequest = supplementaryData.getRequestData().get(Operation.INC.getOperationName());
        if (incrementRequest != null && incrementRequest.size() > 0) {
            this.supplementaryDataRepository.incrementSupplementaryData(caseReference, incrementRequest);
        }
    }

    private void setData(String caseReference, SupplementaryDataUpdateRequest supplementaryData) {
        Map<String, Object> setRequest = supplementaryData.getRequestData().get(Operation.SET.getOperationName());
        if (setRequest !=  null && setRequest.size() > 0) {
            this.supplementaryDataRepository.setSupplementaryData(caseReference, setRequest);
        }
    }
}
