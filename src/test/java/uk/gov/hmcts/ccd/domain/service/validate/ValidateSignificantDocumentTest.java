package uk.gov.hmcts.ccd.domain.service.validate;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateSignificantDocumentTest {

    public static final String SOME_DESCRIPTION = "Some description";
    public static final String VALID_URL = "http://www.yaoo.com";
    public static final String DOCUMENT = "DOCUMENT";
    public static final String EMPTY_DESCRIPTION = "";
    public static final String INVALID_URL = "http://www-com/lll";
    public static final String DESCRIPTION_ERROR_TEXT =
            "Description should not be empty but also not more than 64 characters";
    public static final String INVALID_DOCUMENT_STRING = "DOCUMENTxxx";
    public static final String SIGNIFICANT_ITEM_TYPE_INCORRECT_ERROR_TEXT = "Significant Item type incorrect";
    public static final String URL_FROM_SIGNIFICANT_ITEM_INVALID = "URL from significant item invalid";
    public static final String DESCRIPTION_GREATER_THAN_64 =
            "A DESCRIPTION THAT IS MUCH MUCH MUCH MUCH GREATER THAN 64 CHARACTERS";

    @Test
    public void validateSignificantItemHappyPath() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setDescription(SOME_DESCRIPTION);
        significantItem.setUrl(VALID_URL);
        significantItem.setType(DOCUMENT);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        callbackResponse.setSignificantItem(significantItem);
        assertNull(aboutToSubmitCallbackResponse.getSignificantItem());
        ValidateSignificantDocument.validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        assertTrue(callbackResponse.getErrors().isEmpty());
        assertNotNull(aboutToSubmitCallbackResponse.getSignificantItem());
        assertEquals(SOME_DESCRIPTION, aboutToSubmitCallbackResponse.getSignificantItem().getDescription());
        assertEquals(VALID_URL, aboutToSubmitCallbackResponse.getSignificantItem().getUrl());
        assertEquals(DOCUMENT, aboutToSubmitCallbackResponse.getSignificantItem().getType());
    }

    @Test
    public void validateSignificantItemIncorrectURL() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setDescription(SOME_DESCRIPTION);
        significantItem.setUrl(INVALID_URL);
        significantItem.setType(DOCUMENT);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        callbackResponse.setSignificantItem(significantItem);

        ValidateSignificantDocument.validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        assertFalse(callbackResponse.getErrors().isEmpty());
        List<String> errors = callbackResponse.getErrors();
        assertEquals(1, errors.size());
        assertEquals(URL_FROM_SIGNIFICANT_ITEM_INVALID, errors.get(0));
    }

    @Test
    public void validateSignificantItemEmptyDescription() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setDescription(EMPTY_DESCRIPTION);
        significantItem.setUrl(VALID_URL);
        significantItem.setType(DOCUMENT);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        callbackResponse.setSignificantItem(significantItem);

        ValidateSignificantDocument.validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        assertFalse(callbackResponse.getErrors().isEmpty());
        List<String> errors = callbackResponse.getErrors();
        assertEquals(1, errors.size());
        assertEquals(DESCRIPTION_ERROR_TEXT, errors.get(0));
    }


    @Test
    public void validateSignificantItemIncorrectDocumentAndURL() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setDescription(SOME_DESCRIPTION);
        significantItem.setUrl(INVALID_URL);
        significantItem.setType(INVALID_DOCUMENT_STRING);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        callbackResponse.setSignificantItem(significantItem);

        ValidateSignificantDocument.validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        assertFalse(callbackResponse.getErrors().isEmpty());
        List<String> errors = callbackResponse.getErrors();
        assertEquals(2, errors.size());
        assertEquals(SIGNIFICANT_ITEM_TYPE_INCORRECT_ERROR_TEXT, errors.get(0));
        assertEquals(URL_FROM_SIGNIFICANT_ITEM_INVALID, errors.get(1));
    }

    @Test
    public void validateSignificantItemIncorrectDocumentGreaterThan64() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setDescription(DESCRIPTION_GREATER_THAN_64);
        significantItem.setUrl(INVALID_URL);
        significantItem.setType(INVALID_DOCUMENT_STRING);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        callbackResponse.setSignificantItem(significantItem);

        ValidateSignificantDocument.validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        assertFalse(callbackResponse.getErrors().isEmpty());
        List<String> errors = callbackResponse.getErrors();
        assertEquals(3, errors.size());
        assertEquals(SIGNIFICANT_ITEM_TYPE_INCORRECT_ERROR_TEXT, errors.get(0));
        assertEquals(URL_FROM_SIGNIFICANT_ITEM_INVALID, errors.get(1));
    }

    @Test
    public void validateSignificantItemAndEmptyTypeIncorrectDocumentGreaterThan64() {
        final CallbackResponse callbackResponse = new CallbackResponse();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setDescription(DESCRIPTION_GREATER_THAN_64);
        significantItem.setUrl(INVALID_URL);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

        callbackResponse.setSignificantItem(significantItem);

        ValidateSignificantDocument.validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
        assertFalse(callbackResponse.getErrors().isEmpty());
        List<String> errors = callbackResponse.getErrors();
        assertEquals(3, errors.size());
        assertEquals(SIGNIFICANT_ITEM_TYPE_INCORRECT_ERROR_TEXT, errors.get(0));
        assertEquals(URL_FROM_SIGNIFICANT_ITEM_INVALID, errors.get(1));
    }
}
