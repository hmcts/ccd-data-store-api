package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLinksResource;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TestingSupportControllerTestIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;

    @Inject
    private CaseLinkRepository caseLinkRepository;

    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Before
    public void setUp() {
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
        wireMockServer.resetAll();
    }

    @Test
    public void testGetCaseLinkNoResults() throws Exception {
        final String caseReference = "9816494993793181";
        final String url = "/testing-support/case-link/" + caseReference;

        final MvcResult mvcResult = mockMvc.perform(get(url).header("experimental", "true"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseLinksResource caseLinksResource = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseLinksResource.class);

        assertTrue(caseLinksResource.getCaseLinks().isEmpty());
    }


    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    @Test
    public void testGetCaseLinkReturnsResult() throws Exception {
        final String caseReference = "3393027116986763";
        final String url = "/testing-support/case-link/" + caseReference;

        final MvcResult mvcResult = mockMvc.perform(get(url).header("experimental", "true"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseLinksResource caseLinksResource = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseLinksResource.class);

        assertEquals(1,caseLinksResource.getCaseLinks().size());
        assertEquals(3L, caseLinksResource.getCaseLinks().get(0).getLinkedCaseId());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    @Test
    public void testGetCaseLinkReturnsMultipleResults() throws Exception {
        CaseLinkEntity caseLinkEntity = new CaseLinkEntity(21L, 4L, "Test");
        caseLinkRepository.save(caseLinkEntity);

        final String caseReference = "3393027116986763";
        final String url = "/testing-support/case-link/" + caseReference;

        final MvcResult mvcResult = mockMvc.perform(get(url).header("experimental", "true"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseLinksResource caseLinksResource = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseLinksResource.class);

        final List<CaseLink> caseLinks = caseLinksResource.getCaseLinks();
        final List<Long> foundCaseLinks = caseLinks.stream()
            .map(caseLink -> caseLink.getLinkedCaseId())
            .collect(Collectors.toList());
        assertEquals(2, foundCaseLinks.size());
        assertTrue(foundCaseLinks.containsAll(List.of(3L, 4L)));
    }
}
