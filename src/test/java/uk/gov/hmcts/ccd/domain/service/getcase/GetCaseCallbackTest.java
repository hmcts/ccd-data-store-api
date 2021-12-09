package uk.gov.hmcts.ccd.domain.service.getcase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.callbacks.GetCaseCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetCaseCallbackTest {

    private AutoCloseable closeable;

    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private ResponseEntity<GetCaseCallbackResponse> response;
    @Mock
    private CaseTypeDefinition caseTypeDefinition;
    @Mock
    private CaseDetails caseDetails;
    private GetCaseCallbackResponse getCaseCallbackResponse;

    private GetCaseCallback getCaseCallback;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        getCaseCallback = new GetCaseCallback(callbackInvoker);

        getCaseCallbackResponse = new GetCaseCallbackResponse();
        getCaseCallbackResponse.setMetadataFields(emptyList());

        doReturn(response).when(callbackInvoker)
            .invokeGetCaseCallback(caseTypeDefinition, caseDetails);
        doReturn(getCaseCallbackResponse).when(response).getBody();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("should return GetCaseCallbackResponse with metadataFields")
    void shouldReturnGetCaseCallbackResponseWithMetadataFields() {
        List<CaseViewField> metadataFields = List.of(caseViewField("fieldA", "fieldAValue"));
        getCaseCallbackResponse.setMetadataFields(metadataFields);

        GetCaseCallbackResponse result = getCaseCallback.invoke(caseTypeDefinition, caseDetails, emptyList());

        assertAll(
            () -> assertNotNull(result),
            () -> assertNotNull(result.getMetadataFields()),
            () -> assertThat(result.getMetadataFields(), sameInstance(metadataFields)),
            () -> verify(callbackInvoker)
                .invokeGetCaseCallback(caseTypeDefinition, caseDetails)
        );
    }

    @Test
    @DisplayName("should throw CallbackException when GetCaseCallbackResponse body is null")
    void shouldThrowCallbackExceptionWhenGetCaseCallbackResponseBodyIsNull() {
        String errorMessage = "CCD_CDI_CallbackGetCaseUrl: response body not set";
        doReturn(null).when(response).getBody();

        CallbackException callbackException = assertThrows(CallbackException.class,
            () -> getCaseCallback.invoke(caseTypeDefinition, caseDetails, emptyList()));
        assertTrue(callbackException.getMessage()
            .endsWith(errorMessage));
    }

    @Test
    @DisplayName("should throw CallbackException when callback metadataFields contains existing field")
    void shouldThrowCallbackExceptionWhenCallbackMetadataFieldsContainsExistingField() {
        List<CaseViewField> existingMetadata = List.of(caseViewField("[CASE_TYPE]", "divorce"));
        List<CaseViewField> metadataFields = List.of(caseViewField("[CASE_TYPE]", "probate"));
        getCaseCallbackResponse.setMetadataFields(metadataFields);

        assertThrows(CallbackException.class,
            () -> getCaseCallback.invoke(caseTypeDefinition, caseDetails, existingMetadata));
    }

    @Test
    @DisplayName("should throw CallbackException when callback throws an Exception")
    void shouldThrowCallbackExceptionWhenCallbackThrowsAnException() {
        String errorMessage = "error!";
        when(callbackInvoker.invokeGetCaseCallback(caseTypeDefinition, caseDetails))
            .thenThrow(new RuntimeException(errorMessage));

        CallbackException callbackException = assertThrows(CallbackException.class,
            () -> getCaseCallback.invoke(caseTypeDefinition, caseDetails, emptyList()));
        assertTrue(callbackException.getMessage()
            .endsWith(errorMessage));
    }

    private CaseViewField caseViewField(String fieldId, String fieldValue) {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(fieldId);
        caseViewField.setValue(fieldValue);
        return caseViewField;
    }
}
