package uk.gov.hmcts.ccd.datastore.tests;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import uk.gov.hmcts.ccd.OAuth2Params;
import uk.gov.hmcts.ccd.datastore.tests.helper.CCDHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.S2SHelper;
import uk.gov.hmcts.ccd.idam.IdamApi;
import uk.gov.hmcts.ccd.idam.IdamApiProvider;
import uk.gov.hmcts.ccd.idam.IdamHelper;

public enum AATHelper {

    INSTANCE;

    private final IdamHelper idamHelper;
    private final S2SHelper s2SHelper;
    private final CCDHelper ccdHelper;

    AATHelper() {
        OAuth2Params oAuth2Params = new OAuth2Params() {
            @Override
            public String getIdamBaseURL() {
                return Env.require("IDAM_URL");
            }

            @Override
            public String getOauth2RedirectUrl() {
                return Env.require("OAUTH2_REDIRECT_URI");
            }

            @Override
            public String getOauth2ClientId() {
                return Env.require("OAUTH2_CLIENT_ID");
            }

            @Override
            public String getOauth2ClientSecret() {
                return Env.require("OAUTH2_CLIENT_SECRET");
            }

        };
        IdamApiProvider idamApi = new IdamApiProvider(oAuth2Params) {
            public IdamApi provide() {
                return Feign.builder()
                    .encoder(new JacksonEncoder())
                    .decoder(new JacksonDecoder())
                    .target(IdamApi.class, oAuth2Params.getIdamBaseURL());
            }
        };
        idamHelper = new IdamHelper(oAuth2Params, idamApi);
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
        return Env.require("CCD_GW_SERVICE_NAME");
    }

    public String getGatewayServiceSecret() {
        return Env.require("CCD_GW_SERVICE_SECRET");
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
