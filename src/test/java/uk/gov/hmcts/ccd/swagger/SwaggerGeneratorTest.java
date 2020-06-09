package uk.gov.hmcts.ccd.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
public class SwaggerGeneratorTest {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @BeforeEach
    public void setup() {
        this.mvc = webAppContextSetup(webAppContext).build();
    }

    @DisplayName("Generate swagger documentation for v1 external APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateV1ExternalSpecsDocument() throws Exception {
        generateSpecsFor("v1_external");
    }

    @DisplayName("Generate swagger documentation for v2 external APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateV2ExternalSpecsDocument() throws Exception {
        generateSpecsFor("v2_external");
    }

    @DisplayName("Generate swagger documentation for v1 internal APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateV1InternalSpecsDocument() throws Exception {
        generateSpecsFor("v1_internal");
    }

    @DisplayName("Generate swagger documentation for v2 internal APIs")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateV2InternalSpecsDocument() throws Exception {
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
