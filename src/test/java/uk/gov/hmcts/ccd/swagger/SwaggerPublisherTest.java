package uk.gov.hmcts.ccd.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import uk.gov.hmcts.ccd.WireMockBaseTest;

public class SwaggerPublisherTest extends WireMockBaseTest {

    private MockMvc mvc;

    @Inject
    private WebApplicationContext webAppContext;

    @Before
    public void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }


    @DisplayName("Generate swagger documentation")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void generateDocs() throws Exception {
        ResultActions perform = mvc.perform(get("/v2/api-docs"));
        byte[] specs = perform
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs.json"))) {
            outputStream.write(specs);
        }

    }
}
