package uk.gov.hmcts.ccd.datastore.util;

import uk.gov.hmcts.befta.exception.FunctionalTestException;

public class CaseIdHelper {

    private CaseIdHelper() {
    }

    public static String hypheniseACaseId(final String caseId) {

        if (caseId.length() == 16) {
            final String[] strSubstrings = caseId.split("(?<=\\G.{4})");
            return strSubstrings[0] + "-" + strSubstrings[1] + "-" + strSubstrings[2] + "-" + strSubstrings[3];
        } else {
            throw new FunctionalTestException("Problem getting case id as hypensedFormat");
        }
    }
}
