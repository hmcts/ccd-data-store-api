package uk.gov.hmcts.ccd.domain.service.validate;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;

import java.util.ArrayList;
import java.util.List;

public class ValidateSignificantDocument {

    private static final UrlValidator URL_VALIDATOR = UrlValidator.getInstance();
    private static final int MIN_LENGTH_OF_DESCRIPTION = 0;
    private static final int MAX_LENGTH_OF_DESCRIPTION = 65;

    private ValidateSignificantDocument(){
    }

    public static void validateSignificantItem(AboutToSubmitCallbackResponse response,
                                               CallbackResponse callbackResponse) {
        final SignificantItem significantItem = callbackResponse.getSignificantItem();
        final List<String> errors = new ArrayList<>();

        if (significantItem != null) {
            if (StringUtils.isEmpty(significantItem.getType())
                || (StringUtils.isNotEmpty(significantItem.getType())
                && !significantItem.getType().equals(SignificantItemType.DOCUMENT.name()))) {
                errors.add("Significant Item type incorrect");
            }
            if (!URL_VALIDATOR.isValid(significantItem.getUrl())) {
                errors.add("URL from significant item invalid");
            }
            if (isDescriptionEmptyOrNotWithInSpecifiedRange(significantItem)) {
                errors.add("Description should not be empty but also not more than 64 characters");
            }
            if (errors.isEmpty()) {
                response.setSignificantItem(significantItem);
            } else {
                callbackResponse.setErrors(errors);
            }
        }
    }

    private static boolean isDescriptionEmptyOrNotWithInSpecifiedRange(SignificantItem significantItem) {

        return StringUtils.isEmpty(significantItem.getDescription())
            || (StringUtils.isNotEmpty(significantItem.getDescription())
            && !(significantItem.getDescription().length() > MIN_LENGTH_OF_DESCRIPTION
            && significantItem.getDescription().length() < MAX_LENGTH_OF_DESCRIPTION));
    }
}
