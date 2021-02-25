package uk.gov.hmcts.ccd.domain.model.common;

import java.util.Optional;

public class CaseReferenceUtils {

    public  static final String  CASE_REFERENCE_EXPRESSION = "(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)";

    private CaseReferenceUtils() {
    }

    public static String removeHyphens(String caseReference) {
        if (caseReference.contains("-")) {
            return String.join("", caseReference.split("-"));
        }
        return caseReference;
    }

    public static boolean isAValidCaseReferenceFormat(final String value) {
        return value.matches(CASE_REFERENCE_EXPRESSION);
    }

    public static Optional<String> getFormatCaseReference(Optional<String> caseReference) {

        if (caseReference.isPresent()) {
            if (!CaseReferenceUtils.isAValidCaseReferenceFormat(caseReference.get())) {
                return Optional.of("0000000000000000");
            }
            return Optional.of(CaseReferenceUtils.removeHyphens(caseReference.get()));
        }
        return caseReference;
    }
}
