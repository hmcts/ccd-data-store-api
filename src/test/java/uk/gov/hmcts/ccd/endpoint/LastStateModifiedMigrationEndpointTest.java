package uk.gov.hmcts.ccd.endpoint;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LastStateModifiedMigrationEndpointTest extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/last_state_modified_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoChangesToPostStateForCitizen() throws Exception {

        List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertThat(caseDetailsList).filteredOn(c -> c.getLastStateModifiedDate() == null).size().isEqualTo(2);

        final MvcResult mvcResult = mockMvc.perform(post("/last-state-modified/migrate?jurisdiction=PROBATE")
            .contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(200))
            .andReturn();


        caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertThat(caseDetailsList).filteredOnNull("lastStateModifiedDate").hasSize(0);

        assertThat(caseDetailsList).filteredOn(c -> c.getReference() == 1504259907353529L)
            .extracting("lastStateModifiedDate")
            .containsOnly(LocalDateTime.parse("2017-05-09T15:31:43", ISO_LOCAL_DATE_TIME));

        assertThat(caseDetailsList).filteredOn(c -> c.getReference() == 1504259907353530L)
            .extracting("lastStateModifiedDate")
            .containsOnly(LocalDateTime.parse("2017-05-16T10:31:43", ISO_LOCAL_DATE_TIME));
    }
}
