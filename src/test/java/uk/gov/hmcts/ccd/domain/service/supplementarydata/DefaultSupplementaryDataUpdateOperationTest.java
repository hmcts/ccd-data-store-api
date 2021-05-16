package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSupplementaryDataUpdateOperationTest {

    private ObjectMapper mapper = new ObjectMapper();

    private static final String CASE_REFERENCE = "12345677";

    private DefaultSupplementaryDataUpdateOperation defaultSupplementaryDataOperation;

    @Mock
    private SupplementaryDataRepository supplementaryDataRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        this.defaultSupplementaryDataOperation =
            new DefaultSupplementaryDataUpdateOperation(supplementaryDataRepository);
    }

    @Test
    void shouldNotInvokeRepository() {
        Map<String, Map<String, Object>> supplementaryDataRequest = new HashMap<>();
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest(supplementaryDataRequest);
        this.defaultSupplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE, request);

        assertAll(
            () -> verify(supplementaryDataRepository, times(0)).setSupplementaryData(anyString(), anyString(),
                any(Object.class)),
            () -> verify(supplementaryDataRepository, times(0)).incrementSupplementaryData(anyString(), anyString(),
                any(Object.class)),
            () -> verify(supplementaryDataRepository, times(1)).findSupplementaryData(anyString(), anySet())
        );
    }

    @Test
    void invokeRepositoryOnSupplementaryData() {
        Map<String, Map<String, Object>> setMap = createSupplementaryDataSetRequest();
        Map<String, Map<String, Object>> incMap = createSupplementaryDataIncrementRequest();

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.putAll(incMap.get(SupplementaryDataOperation.INC.getOperationName()));
        resultMap.putAll(setMap.get(SupplementaryDataOperation.SET.getOperationName()));
        SupplementaryData supplementaryData = new SupplementaryData(resultMap);
        when(this.supplementaryDataRepository.findSupplementaryData(anyString(), anySet()))
            .thenReturn(supplementaryData);

        Map<String, Map<String, Object>> supplementaryDataRequest = new HashMap<>();
        supplementaryDataRequest.putAll(setMap);
        supplementaryDataRequest.putAll(incMap);
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest(supplementaryDataRequest);
        SupplementaryData response = this.defaultSupplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE,
            request);

        assertAll(
            () -> verify(supplementaryDataRepository, times(1)).incrementSupplementaryData(anyString(), anyString(),
                any(Object.class)),
            () -> verify(supplementaryDataRepository, times(1)).findSupplementaryData(anyString(), anySet()),
            () -> assertThat(response.getResponse(), is(resultMap))
        );
    }

    private Map<String, Map<String, Object>> createSupplementaryDataSetRequest() {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 10\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private Map<String, Map<String, Object>> createSupplementaryDataIncrementRequest() {
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 3\n"
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
