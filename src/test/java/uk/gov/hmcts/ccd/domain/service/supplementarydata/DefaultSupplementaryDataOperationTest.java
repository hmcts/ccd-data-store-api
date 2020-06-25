package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSupplementaryDataOperationTest {

    private String CASE_REFERENCE = "12345677";

    private DefaultSupplementaryDataOperation defaultSupplementaryDataOperation;

    @Mock
    private SupplementaryDataRepository supplementaryDataRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        this.defaultSupplementaryDataOperation = new DefaultSupplementaryDataOperation(supplementaryDataRepository);
    }

    @Test
    void shouldNotInvokeRepository() {
        Map<String, Map<String, Object>> supplementaryDataRequest = new HashMap<>();
        SupplementaryDataRequest request = new SupplementaryDataRequest(supplementaryDataRequest);
        this.defaultSupplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE, request);

        assertAll(
            () -> verify(supplementaryDataRepository, times(0)).setSupplementaryData(anyString(), anyMap()),
            () -> verify(supplementaryDataRepository, times(0)).incrementSupplementaryData(anyString(), anyMap()),
            () -> verify(supplementaryDataRepository, times(1)).findSupplementaryData(anyString())
        );
    }

    @Test
    void invokeRepositoryOnSupplementaryData() {
        Map<String, Map<String, Object>> supplementaryDataRequest = new HashMap<>();
        Map<String, Object> setMap = new HashMap<>();
        setMap.put("test1", "test value1");
        setMap.put("test2", "test value2");
        supplementaryDataRequest.put("$set", setMap);

        Map<String, Object> incMap = new HashMap<>();
        incMap.put("test3", 2);
        supplementaryDataRequest.put("$inc", incMap);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.putAll(incMap);
        resultMap.putAll(setMap);
        SupplementaryData supplementaryData = new SupplementaryData(resultMap);
        when(this.supplementaryDataRepository.findSupplementaryData(anyString())).thenReturn(supplementaryData);

        SupplementaryDataRequest request = new SupplementaryDataRequest(supplementaryDataRequest);
        SupplementaryData response = this.defaultSupplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE, request);

        assertAll(
            () -> verify(supplementaryDataRepository, times(1)).setSupplementaryData(anyString(), anyMap()),
            () -> verify(supplementaryDataRepository, times(1)).incrementSupplementaryData(anyString(), anyMap()),
            () -> verify(supplementaryDataRepository, times(1)).findSupplementaryData(anyString()),
            () -> assertThat(response.getSupplementaryData(), is(resultMap))
        );
    }
}
