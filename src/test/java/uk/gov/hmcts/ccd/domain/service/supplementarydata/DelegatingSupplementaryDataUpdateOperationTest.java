package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegatingSupplementaryDataUpdateOperationTest {

    private static final String CASE_REFERENCE = "1234567890123456";

    @Mock
    private PersistenceStrategyResolver persistenceResolver;

    @Mock
    private DefaultSupplementaryDataUpdateOperation defaultSupplementaryDataUpdateOperation;

    @Mock
    private ServicePersistenceClient servicePersistenceClient;

    private DelegatingSupplementaryDataUpdateOperation delegatingOperation;

    @BeforeEach
    void setUp() {
        delegatingOperation = new DelegatingSupplementaryDataUpdateOperation(
            persistenceResolver,
            defaultSupplementaryDataUpdateOperation,
            servicePersistenceClient
        );
    }

    @Test
    void shouldDelegateToDefaultOperationWhenCaseIsCentralised() {
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();
        SupplementaryData expected = new SupplementaryData();

        when(persistenceResolver.isDecentralised(Long.valueOf(CASE_REFERENCE))).thenReturn(false);
        when(defaultSupplementaryDataUpdateOperation.updateSupplementaryData(CASE_REFERENCE, request))
            .thenReturn(expected);

        SupplementaryData result = delegatingOperation.updateSupplementaryData(CASE_REFERENCE, request);

        assertSame(expected, result);
        verify(defaultSupplementaryDataUpdateOperation).updateSupplementaryData(CASE_REFERENCE, request);
        verify(servicePersistenceClient, never())
            .updateSupplementaryData(anyLong(), any(SupplementaryDataUpdateRequest.class));
    }

    @Test
    void shouldInvokeServicePersistenceClientWhenCaseIsDecentralised() {
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest(
            Map.of("$set", Map.of("fieldA", 10))
        );
        ObjectNode responseNode = JsonNodeFactory.instance.objectNode().put("fieldA", 10);

        when(persistenceResolver.isDecentralised(Long.valueOf(CASE_REFERENCE))).thenReturn(true);
        when(servicePersistenceClient.updateSupplementaryData(Long.valueOf(CASE_REFERENCE), request))
            .thenReturn(responseNode);

        SupplementaryData result = delegatingOperation.updateSupplementaryData(CASE_REFERENCE, request);


        // Responses should contain just the keys and values that were updated
        assertEquals(Map.of("fieldA", 10), result.getResponse());
        verify(servicePersistenceClient).updateSupplementaryData(eq(Long.valueOf(CASE_REFERENCE)), eq(request));
        verify(defaultSupplementaryDataUpdateOperation, never())
            .updateSupplementaryData(anyString(), any(SupplementaryDataUpdateRequest.class));
    }
}
