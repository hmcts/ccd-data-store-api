package uk.gov.hmcts.ccd.datastore.tests;

import uk.gov.hmcts.ccd.datastore.tests.helper.CCDHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.S2SHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.IdamHelper;

public enum AATHelper {

    INSTANCE;

    private final IdamHelper idamHelper;
    private final S2SHelper s2SHelper;
    private final CCDHelper ccdHelper;

    AATHelper() {
        idamHelper = new IdamHelper(getIdamURL());
        s2SHelper = new S2SHelper(getS2SURL(), getGatewayServiceSecret(), getGatewayServiceName());
        ccdHelper = new CCDHelper();
    }

    public String getTestUrl() {
        return System.getenv("TEST_URL");
    }

    public String getIdamURL() {
        return System.getenv("IDAM_URL");
    }

    public String getS2SURL() {
        return System.getenv("S2S_URL");
    }

    public String getCaseworkerAutoTestEmail() {
        return System.getenv("CCD_CASEWORKER_AUTOTEST_EMAIL");
    }

    public String getCaseworkerAutoTestPassword() {
        return System.getenv("CCD_CASEWORKER_AUTOTEST_PASSWORD");
    }

    public String getGatewayServiceName() {
        return System.getenv("CCD_GW_SERVICE_NAME");
    }

    public String getGatewayServiceSecret() {
        return System.getenv("CCD_GW_SERVICE_SECRET");
    }

    public IdamHelper getIdamHelper() {
        return idamHelper;
    }

    public S2SHelper getS2SHelper() {
        return s2SHelper;
    }

    public CCDHelper getCcdHelper() {
        return ccdHelper;
    }
}
