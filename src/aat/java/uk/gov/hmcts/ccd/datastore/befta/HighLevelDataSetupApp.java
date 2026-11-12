package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.auth.UserTokenProviderConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final String MASTER_CASEWORKER_EMAIL = "master.caseworker@gmail.com";
    private static final String CASEWORKER_AUTOTEST_PASSWORD_ENV = "CCD_CASEWORKER_AUTOTEST_PASSWORD";
    private static final int DATA_STORE_READINESS_ATTEMPTS = 45;
    private static final long DATA_STORE_READINESS_POLL_INTERVAL_MILLIS = 1_000L;
    private static final DefinitionReadinessSpec RICH_TEXT_AREA_READINESS_SPEC = new DefinitionReadinessSpec(
        "FT_MasterCaseType",
        "createCase",
        "caseworker-befta_master",
        MASTER_CASEWORKER_EMAIL,
        CASEWORKER_AUTOTEST_PASSWORD_ENV,
        List.of(
            new DefinitionReadinessSpec.RequiredField("RichTextAreaField", "RichTextArea"),
            new DefinitionReadinessSpec.RequiredField("RichTextAreaMinField", "RichTextArea")
        ),
        List.of("createCase", "updateCase")
    );
    private static final List<DefinitionReadinessSpec> REQUIRED_DEFINITIONS = List.of(
        RICH_TEXT_AREA_READINESS_SPEC
    );

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
        REQUIRED_DEFINITIONS.forEach(this::verifyDefinitionIsAvailable);
        REQUIRED_DEFINITIONS.forEach(this::waitUntilDataStoreDefinitionIsReady);
    }

    private void verifyDefinitionIsAvailable(DefinitionReadinessSpec spec) {
        RestAssured.useRelaxedHTTPSValidation();

        Response response = asAutoTestImporter()
            .when()
            .get("/api/data/case-type/{caseTypeId}", spec.caseTypeId());

        DefinitionReadinessVerifier.verify(response, spec);
    }

    private void waitUntilDataStoreDefinitionIsReady(DefinitionReadinessSpec spec) {
        Supplier<RequestSpecification> asReadinessUser = asReadinessUser(spec);
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= DATA_STORE_READINESS_ATTEMPTS; attempt++) {
            try {
                Response response = asReadinessUser.get()
                    .given()
                    .pathParam("caseTypeId", spec.caseTypeId())
                    .pathParam("triggerId", spec.dataStoreReadinessEventId())
                    .accept(V2.MediaType.CASE_TYPE_UPDATE_VIEW_EVENT)
                    .header(V2.EXPERIMENTAL_HEADER, "true")
                    .when()
                    .get("/internal/case-types/{caseTypeId}/event-triggers/{triggerId}?ignore-warning=true");

                verifyDataStoreStartTrigger(response, spec);
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt < DATA_STORE_READINESS_ATTEMPTS) {
                    waitBeforeNextDataStoreReadinessAttempt();
                }
            }
        }

        throw new IllegalStateException("Data Store did not serve the required fields for "
            + spec.caseTypeId() + " " + spec.dataStoreReadinessEventId() + " after "
            + DATA_STORE_READINESS_ATTEMPTS + " attempts. The service is likely still using a cached case definition "
            + "from before highLevelDataSetup completed.", lastFailure);
    }

    private void verifyDataStoreStartTrigger(Response response, DefinitionReadinessSpec spec) {
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Could not verify Data Store start trigger for "
                + spec.caseTypeId() + " " + spec.dataStoreReadinessEventId()
                + ". Data Store returned HTTP " + response.getStatusCode() + ": " + response.getBody().asString());
        }

        DefinitionReadinessVerifier.verifyVisibleFields(response.jsonPath(), spec);
    }

    private Supplier<RequestSpecification> asReadinessUser(DefinitionReadinessSpec spec) {
        DefaultTestAutomationAdapter adapter = new DefaultTestAutomationAdapter();
        UserData caseworker = new UserData(
            spec.userEmail(),
            EnvironmentVariableUtils.getRequiredVariable(spec.userPasswordEnvironmentVariable())
        );

        try {
            adapter.authenticate(caseworker, UserTokenProviderConfig.DEFAULT_INSTANCE.getClientId());
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not authenticate " + spec.userEmail()
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
