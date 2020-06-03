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

    @DisplayName("Generate swagger documentation for v1 APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateV1SpecsDocument() throws Exception {
        generateSpecsFor("v1");
    }

    @DisplayName("Generate swagger documentation for v2 APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateV2SpecsDocument() throws Exception {
        generateSpecsFor("v2");
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
