package uk.gov.hmcts.ccd.domain.model.common;

import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

public class CaseReferenceUtils {

    private CaseReferenceUtils() {
    }

    public static String formatCaseReference(String caseReference) {
        if (caseReference.contains("-")) {
            return String.join("", caseReference.split("-"));
        }
        return caseReference;
    }

    public static void checkRegex(final String value) {
        final String regularExpression = "(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)";
        if (value.matches(regularExpression)) {
            return;
        }
        throw new ValidationException(String.format("casereference:%s, has not the correct value.", value));
    }
}
