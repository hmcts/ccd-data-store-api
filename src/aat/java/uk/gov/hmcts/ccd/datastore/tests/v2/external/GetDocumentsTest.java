package uk.gov.hmcts.ccd.datastore.tests.v2.external;

import com.launchdarkly.client.LDClient;
import com.launchdarkly.client.LDUser;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.helper.LDHelper;
import uk.gov.hmcts.ccd.v2.V2;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.equalTo;

public class GetDocumentsTest extends BaseTest {

    private static final String caseReference = "1579610355010609";

    private LDClient ldClient;

    protected GetDocumentsTest(AATHelper aat) {
        super(aat);
        this.ldClient = LDHelper.INSTANCE.getClient();
    }

    @Test
    void shouldGetCaseDocuments() {
        LDUser callingServiceUser = new LDUser.Builder("ccd-data-store-api")
            .custom("callingService", "ccd-data-store-api")
            .build();

        boolean isUsingCaseDocumentApi = ldClient.boolVariation("download-documents-using-case-document-access-management-api",
            callingServiceUser, false);

        if (!isUsingCaseDocumentApi) {
            getCaseDocumentsFromDocumentManagementApi();
        } else {
            getCaseDocumentsFromCaseDocumentsAccessManagementApi();
        }
    }

    private void getCaseDocumentsFromDocumentManagementApi() {
        System.out.println("DOWNLOADING FROM DOC STORE");
        asAutoTestCaseworker(caseReference)
            .when()
            .get("/cases/{cid}/documents")
            .then()
            .assertThat()
            .statusCode(200)
            .body("documentResources[0].url", equalTo("http://dm-store-aat.service.core-compute-aat.internal/documents/1d9e2f5f-2114-4748-b01c-70481000ce6d/binary"))
            .body("documentResources[0].name", equalTo("Screenshot 2019-09-26 at 13.06.47.png"))
            .body("documentResources[0].type", equalTo("png"))
            .body("documentResources[0].description", equalTo("Evidence screen capture"))
            .body("_links.self.href", equalTo(String.format("%s/cases/%s/documents?callingService=ccd-data-store-api", aat.getTestUrl(), caseReference)));
    }

    private void getCaseDocumentsFromCaseDocumentsAccessManagementApi() {
        System.out.println("DOWNLOADING FROM CASE DOCUMENT API");
        asAutoTestCaseworker(caseReference)
            .when()
            .get("/cases/{cid}/documents")
            .then()
            .assertThat()
            .statusCode(200)
            .body("documentResources[0].url", equalTo("SAMPLE FROM NEW CASE DOCUMENT API"))
            .body("documentResources[0].name", equalTo("SAMPLE FROM NEW CASE DOCUMENT API"))
            .body("documentResources[0].type", equalTo("SAMPLE FROM NEW CASE DOCUMENT API"))
            .body("documentResources[0].description", equalTo("SAMPLE FROM NEW CASE DOCUMENT API"))
            .body("_links.self.href", equalTo(String.format("%s/cases/%s/documents?callingService=ccd-data-store-api", aat.getTestUrl(), caseReference)));
    }

    private RequestSpecification asAutoTestCaseworker(String caseReference) {
        return asAutoTestCaseworker(FALSE)
            .get()
            .pathParam("cid", caseReference)
            .header("experimental", "true")
            .accept(V2.MediaType.CASE_DOCUMENTS);
    }
}
