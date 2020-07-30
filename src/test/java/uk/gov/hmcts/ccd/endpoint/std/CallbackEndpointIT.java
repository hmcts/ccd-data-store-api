package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CallbackEndpointIT extends WireMockBaseTest {

    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "TestAddressBookCase";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void getPrintableDocumentsShouldLogAudit() throws Exception {

        CaseDetails caseDetails = CaseDetailsBuilder.newCaseDetails().withReference(1535450291607660L).build();
        String url = "/callback/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/documents";

        MvcResult result = mockMvc.perform(post(url)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(caseDetails)))
                .andExpect(status().is(200))
                .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<Document> documents = Arrays.asList(mapper.readValue(responseAsString, Document[].class));

        assertThat(documents, hasSize(1));
        assertThat(documents, hasItem(hasProperty("url", equalTo("http://localhost:3453/print/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1535450291607660"))));
        assertThat(documents, hasItem(hasProperty("name", equalTo("CCD Print"))));
        assertThat(documents, hasItem(hasProperty("type", equalTo("CCD Print Type"))));
        assertThat(documents, hasItem(hasProperty("description", equalTo("Printing for CCD"))));
    }

}
