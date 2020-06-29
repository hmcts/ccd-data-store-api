package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.Operation;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
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

    private ObjectMapper mapper = new ObjectMapper();

    private static final String CASE_REFERENCE = "12345677";

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
        Map<String, Map<String, Object>> setMap = createSupplementaryDataSetRequest();
        Map<String, Map<String, Object>> incMap = createSupplementaryDataIncrementRequest();

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.putAll(incMap.get(Operation.INC.getOperationName()));
        resultMap.putAll(setMap.get(Operation.SET.getOperationName()));
        SupplementaryData supplementaryData = new SupplementaryData(resultMap);
        when(this.supplementaryDataRepository.findSupplementaryData(anyString())).thenReturn(supplementaryData);

        Map<String, Map<String, Object>> supplementaryDataRequest = new HashMap<>();
        supplementaryDataRequest.putAll(setMap);
        supplementaryDataRequest.putAll(incMap);
        SupplementaryDataRequest request = new SupplementaryDataRequest(supplementaryDataRequest);
        SupplementaryData response = this.defaultSupplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE, request);

        assertAll(
            () -> verify(supplementaryDataRepository, times(1)).setSupplementaryData(anyString(), anyMap()),
            () -> verify(supplementaryDataRepository, times(1)).incrementSupplementaryData(anyString(), anyMap()),
            () -> verify(supplementaryDataRepository, times(1)).findSupplementaryData(anyString()),
            () -> assertThat(response.getSupplementaryData(), is(resultMap))
        );
    }

    private Map<String, Map<String, Object>> createSupplementaryDataSetRequest() {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\t\"organisationA\": 10\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private Map<String, Map<String, Object>> createSupplementaryDataIncrementRequest() {
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\t\"organisationB\": 3\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private Map<String, Map<String, Object>> convertData(String jsonRquest) {
        Map<String, Map<String, Object>> requestData;
        try {
            requestData = mapper.readValue(jsonRquest, Map.class);
        } catch (JsonProcessingException e) {
            requestData = new HashMap<>();
        }
        return requestData;
    }
}
