package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.auth.UserTokenProviderConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final String BEFTA_MASTER_CASEWORKER_EMAIL = "master.caseworker@gmail.com";
    private static final String BEFTA_MASTER_CASEWORKER_PASSWORD_ENV = "CCD_BEFTA_MASTER_CASEWORKER_PWD";
    private static final String CREATE_CASE_EVENT = "createCase";
    private static final int DATA_STORE_READINESS_ATTEMPTS = 45;
    private static final long DATA_STORE_READINESS_POLL_INTERVAL_MILLIS = 1_000L;

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment);
    }

    public static void main(String[] args) throws Throwable {
        if (!args[0].toLowerCase(Locale.ENGLISH).equals("prod")) {
            main(HighLevelDataSetupApp.class, args);
        }
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure() {
        return false;
    }

    @Override
    public synchronized void loadDataIfNotLoadedVeryRecently() {
        super.loadDataIfNotLoadedVeryRecently();
        verifyRichTextAreaDefinitionIsAvailable();
        waitUntilDataStoreRichTextAreaDefinitionIsReady();
    }

    private void verifyRichTextAreaDefinitionIsAvailable() {
        RestAssured.useRelaxedHTTPSValidation();

        Response response = asAutoTestImporter()
            .when()
            .get("/api/data/case-type/{caseTypeId}", RichTextAreaDefinitionVerifier.MASTER_CASE_TYPE);

        RichTextAreaDefinitionVerifier.verify(response);
    }

    private void waitUntilDataStoreRichTextAreaDefinitionIsReady() {
        Supplier<RequestSpecification> asBeftaMasterCaseworker = asBeftaMasterCaseworker();
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= DATA_STORE_READINESS_ATTEMPTS; attempt++) {
            try {
                Response response = asBeftaMasterCaseworker.get()
                    .given()
                    .pathParam("caseTypeId", RichTextAreaDefinitionVerifier.MASTER_CASE_TYPE)
                    .pathParam("triggerId", CREATE_CASE_EVENT)
                    .accept(V2.MediaType.CASE_TYPE_UPDATE_VIEW_EVENT)
                    .header(V2.EXPERIMENTAL_HEADER, "true")
                    .when()
                    .get("/internal/case-types/{caseTypeId}/event-triggers/{triggerId}?ignore-warning=true");

                verifyDataStoreRichTextAreaStartTrigger(response);
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt < DATA_STORE_READINESS_ATTEMPTS) {
                    waitBeforeNextDataStoreReadinessAttempt();
                }
            }
        }

        throw new IllegalStateException("Data Store did not serve the RichTextArea fields for "
            + RichTextAreaDefinitionVerifier.MASTER_CASE_TYPE + " " + CREATE_CASE_EVENT + " after "
            + DATA_STORE_READINESS_ATTEMPTS + " attempts. The service is likely still using a cached "
            + "case definition from before highLevelDataSetup completed.", lastFailure);
    }

    private void verifyDataStoreRichTextAreaStartTrigger(Response response) {
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Could not verify Data Store start trigger for "
                + RichTextAreaDefinitionVerifier.MASTER_CASE_TYPE + " " + CREATE_CASE_EVENT
                + ". Data Store returned HTTP " + response.getStatusCode() + ": " + response.getBody().asString());
        }

        RichTextAreaDefinitionVerifier.verifyVisibleFields(response.jsonPath());
    }

    private Supplier<RequestSpecification> asBeftaMasterCaseworker() {
        DefaultTestAutomationAdapter adapter = new DefaultTestAutomationAdapter();
        UserData caseworker = new UserData(
            BEFTA_MASTER_CASEWORKER_EMAIL,
            EnvironmentVariableUtils.getRequiredVariable(BEFTA_MASTER_CASEWORKER_PASSWORD_ENV)
        );

        try {
            adapter.authenticate(caseworker, UserTokenProviderConfig.DEFAULT_INSTANCE.getClientId());
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not authenticate " + BEFTA_MASTER_CASEWORKER_EMAIL
                + " for Data Store definition readiness check.", e);
        }

        String s2sToken = adapter.getNewS2STokenWithEnvVars("CCD_API_GATEWAY_S2S_ID", "CCD_API_GATEWAY_S2S_KEY");

        return () -> RestAssured.given(new RequestSpecBuilder()
                .setBaseUri(BeftaMain.getConfig().getTestUrl())
                .build())
            .header("Authorization", "Bearer " + caseworker.getAccessToken())
            .header("ServiceAuthorization", s2sToken);
    }

    private void waitBeforeNextDataStoreReadinessAttempt() {
        try {
            Thread.sleep(DATA_STORE_READINESS_POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Data Store definition readiness.", e);
        }
    }

}
