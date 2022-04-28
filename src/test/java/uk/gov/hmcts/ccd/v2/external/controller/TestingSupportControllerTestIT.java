package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinksResource;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.NON_STANDARD_LINK;

public class TestingSupportControllerTestIT extends WireMockBaseTest {

    private static final String URL =  "/testing-support/case-link/";

    @Inject
    private WebApplicationContext wac;

    @Inject
    private CaseLinkRepository caseLinkRepository;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        wireMockServer.resetAll();
    }

    @Test
    public void testGetCaseLinkNoResults() throws Exception {
        final String url = URL + CASE_21_REFERENCE;

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
        final String url = URL + CASE_22_REFERENCE;

        final MvcResult mvcResult = mockMvc.perform(get(url).header("experimental", "true"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseLinksResource caseLinksResource = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseLinksResource.class);

        assertEquals(1, caseLinksResource.getCaseLinks().size());
        assertEquals(
            Long.parseLong(CASE_03_REFERENCE), // pre-existing case link (see classpath:sql/insert_cases.sql)
            caseLinksResource.getCaseLinks().get(0).getLinkedCaseReference()
        );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    @Test
    public void testGetCaseLinkReturnsMultipleResults() throws Exception {
        CaseLinkEntity caseLinkEntity = new CaseLinkEntity(CASE_22_ID, CASE_04_ID, "Test", NON_STANDARD_LINK);
        caseLinkRepository.save(caseLinkEntity);

        final String url = URL + CASE_22_REFERENCE;

        final MvcResult mvcResult = mockMvc.perform(get(url).header("experimental", "true"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseLinksResource caseLinksResource = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseLinksResource.class);

        final List<CaseLink> caseLinks = caseLinksResource.getCaseLinks();
        final List<Long> foundCaseLinks = caseLinks.stream()
            .map(CaseLink::getLinkedCaseReference)
            .collect(Collectors.toList());
        assertEquals(2, foundCaseLinks.size());
        assertTrue(foundCaseLinks.containsAll(List.of(
                Long.parseLong(CASE_03_REFERENCE), // pre-existing case link (see classpath:sql/insert_cases.sql)
                Long.parseLong(CASE_04_REFERENCE)  // new case link
            )));
    }
}
