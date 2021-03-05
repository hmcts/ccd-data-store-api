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
}
