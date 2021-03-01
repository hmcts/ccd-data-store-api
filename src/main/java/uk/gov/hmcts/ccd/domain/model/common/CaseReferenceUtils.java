package uk.gov.hmcts.ccd.domain.model.common;

public class CaseReferenceUtils {

    public static final String CASE_REFERENCE_REGEX = "(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)";

    private CaseReferenceUtils() {
    }

    public static String removeHyphens(String caseReference) {
        if (caseReference.contains("-")) {
            return String.join("", caseReference.split("-"));
        }
        return caseReference;
    }

    public static boolean isAValidCaseReference(final String value) {
        return value.matches(CASE_REFERENCE_REGEX);
    }
}
