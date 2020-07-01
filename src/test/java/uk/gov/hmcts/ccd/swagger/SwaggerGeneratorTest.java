package uk.gov.hmcts.ccd.swagger;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

public class SwaggerGeneratorTest extends WireMockBaseTest {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @Before
    public void setup() {
        this.mvc = webAppContextSetup(webAppContext).build();
    }

    @DisplayName("Generate swagger documentation for v1 external APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateV1ExternalSpecsDocument() throws Exception {
        generateSpecsFor("v1_external");
    }

    @DisplayName("Generate swagger documentation for v2 external APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateV2ExternalSpecsDocument() throws Exception {
        generateSpecsFor("v2_external");
    }

    @DisplayName("Generate swagger documentation for v1 internal APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateV1InternalSpecsDocument() throws Exception {
        generateSpecsFor("v1_internal");
    }

    @DisplayName("Generate swagger documentation for v2 internal APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateV2InternalSpecsDocument() throws Exception {
        generateSpecsFor("v2_internal");
    }

    private void generateSpecsFor(String groupName) throws Exception {
        ResultActions perform = mvc.perform(get("/v2/api-docs?group=" + groupName));
        byte[] specs = perform
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files
                .newOutputStream(Paths.get("/tmp/ccd-data-store-api." + groupName + ".json"))) {
            outputStream.write(specs);
        }
    }
}
