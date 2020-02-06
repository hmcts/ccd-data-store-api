package uk.gov.hmcts.ccd.datastore.tests;

import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.datastore.tests.helper.CCDHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.S2SHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.IdamHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.OAuth2;

import java.util.function.Supplier;

public enum AATHelper {

    INSTANCE;

    private final IdamHelper idamHelper;
    private final S2SHelper s2SHelper;
    private final CCDHelper ccdHelper;

    AATHelper() {
        idamHelper = new IdamHelper(getIdamURL(), OAuth2.INSTANCE);
        s2SHelper = new S2SHelper(getS2SURL(), getGatewayServiceSecret(), getGatewayServiceName());
        ccdHelper = new CCDHelper();
    }

    public String getTestUrl() {
        return Env.require("TEST_URL");
    }

    public String getIdamURL() {
        return Env.require("IDAM_URL");
    }

    public String getS2SURL() {
        return Env.require("S2S_URL");
    }

    public String getGatewayServiceName() {
        return Env.require("BEFTA_S2S_CLIENT_ID");
    }

    public String getGatewayServiceSecret() {
        return Env.require("BEFTA_S2S_CLIENT_SECRET");
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

    public String generateTokenUpdateCase(Supplier<RequestSpecification> asUser,
                                          String jurisdiction,
                                          String caseType,
                                          Long caseReference,
                                          String event) {
        return ccdHelper.generateTokenUpdateCase(asUser, jurisdiction, caseType, caseReference, event);
    }

    public String getCaseworkerAutoTestEmail() {
        return Env.require("CCD_CASEWORKER_AUTOTEST_EMAIL");
    }

    public String getCaseworkerAutoTestPassword() {
        return Env.require("CCD_CASEWORKER_AUTOTEST_PASSWORD");
    }

    public String getDefinitionStoreUrl() {
        return Env.require("DEFINITION_STORE_HOST");
    }

    public String getImporterAutoTestEmail() {
        return Env.require("CCD_IMPORT_AUTOTEST_EMAIL");
    }

    public String getImporterAutoTestPassword() {
        return Env.require("CCD_IMPORT_AUTOTEST_PASSWORD");
    }

    public String getPrivateCaseworkerEmail() {
        return Env.require("CCD_PRIVATE_CASEWORKER_EMAIL");
    }

    public String getPrivateCaseworkerPassword() {
        return Env.require("CCD_PRIVATE_CASEWORKER_PASSWORD");
    }

    public String getRestrictedCaseworkerEmail() {
        return Env.require("CCD_RESTRICTED_CASEWORKER_EMAIL");
    }

    public String getRestrictedCaseworkerPassword() {
        return Env.require("CCD_RESTRICTED_CASEWORKER_PASSWORD");
    }

    public String getPrivateCaseworkerSolicitorEmail() {
        return Env.require("CCD_PRIVATE_CASEWORKER_SOLICITOR_EMAIL");
    }

    public String getPrivateCaseworkerSolicitorPassword() {
        return Env.require("CCD_PRIVATE_CASEWORKER_SOLICITOR_PASSWORD");
    }

    public String getPrivateCrossCaseTypeCaseworkerEmail() {
        return Env.require("CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_EMAIL");
    }

    public String getPrivateCrossCaseTypeCaseworkerPassword() {
        return Env.require("CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_PASSWORD");
    }

    public String getPrivateCrossCaseTypeSolicitorEmail() {
        return Env.require("CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_EMAIL");
    }

    public String getPrivateCrossCaseTypeSolicitorPassword() {
        return Env.require("CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_PASSWORD");
    }

    public String getRestrictedCrossCaseTypeCaseworkerEmail() {
        return Env.require("CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_EMAIL");
    }

    public String getRestrictedCrossCaseTypeCaseworkerPassword() {
        return Env.require("CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_PASSWORD");
    }

}
