package uk.gov.hmcts.ccd.domain.model.common;

public class CaseReferenceUtils {

    private CaseReferenceUtils() {
    }

    public static String formatCaseReference(String caseReference) {
        if (caseReference.contains("-")) {
            return String.join("", caseReference.split("-"));
        }
        return caseReference;
    }

    public static boolean checkRegex(final String value) {
        final String regularExpression = "(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)";
        return value.matches(regularExpression) ;
    }
}
