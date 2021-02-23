package uk.gov.hmcts.ccd.domain.model.common;

import java.util.Optional;

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
        return value.matches(regularExpression);
    }

    public static Optional<String> getFormatCaseReference(Optional<String> caseReference) {

        if (caseReference.isPresent()) {
            if (!CaseReferenceUtils.checkRegex(caseReference.get())) {
                return Optional.of("0000000000000000");
            }
            return Optional.of(CaseReferenceUtils.formatCaseReference(caseReference.get()));
        }
        return caseReference;
    }
}
