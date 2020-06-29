package uk.gov.hmcts.ccd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.ContextCleanupListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

@Configuration
@Profile("test")
@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
class TestConfiguration extends ContextCleanupListener {

    private final ApplicationParams applicationParams;

    private final PostgresUtil postgresUtil;

    private EmbeddedPostgres pg;

    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("checkstyle:LineLength") // don't want to break long regex expressions
    private static final String baseTypes =
        "[\n"
            + "  {\n"
            + "    \"type\": \"Text\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"Number\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"Email\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"YesOrNo\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"Date\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"DateTime\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"FixedList\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"PostCode\",\n"
            + "    \"regular_expression\": \"^([A-PR-UWYZ0-9][A-HK-Y0-9][AEHMNPRTVXY0-9]?[ABEHMNPRVWXY0-9]? {1,2}[0-9][ABD-HJLN-UW-Z]{2}|GIR 0AA)$\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"MoneyGBP\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"PhoneUK\",\n"
            + "    \"regular_expression\": \"^(((\\\\+44\\\\s?\\\\d{4}|\\\\(?0\\\\d{4}\\\\)?)\\\\s?\\\\d{3}\\\\s?\\\\d{3})|((\\\\+44\\\\s?\\\\d{3}|\\\\(?0\\\\d{3}\\\\)?)\\\\s?\\\\d{3}\\\\s?\\\\d{4})|((\\\\+44\\\\s?\\\\d{2}|\\\\(?0\\\\d{2}\\\\)?)\\\\s?\\\\d{4}\\\\s?\\\\d{4}))(\\\\s?\\\\#(\\\\d{4}|\\\\d{3}))?$\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"TextArea\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"Complex\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"type\": \"Collection\"\n"
            + "  },"
            + "  {\n"
            + "    \"type\": \"MultiSelectList\"\n"
            + "  },"
            + "  {\n"
            + "    \"type\": \"Document\"\n"
            + "  }\n"
            + "]";

    @Autowired
    TestConfiguration(final ApplicationParams applicationParams, final PostgresUtil postgresUtil) {
        this.applicationParams = applicationParams;
        this.postgresUtil = postgresUtil;
    }

    @Bean
    @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
    @Primary
    CaseDefinitionRepository caseDefinitionRepository() throws IOException {
        final FieldTypeDefinition[] fieldTypeDefinitions = mapper.readValue(baseTypes.getBytes(), FieldTypeDefinition[].class);
        final DefaultCaseDefinitionRepository caseDefinitionRepository = mock(DefaultCaseDefinitionRepository.class);

        ReflectionTestUtils.setField(caseDefinitionRepository, "applicationParams", applicationParams);
        ReflectionTestUtils.setField(caseDefinitionRepository, "restTemplate", new RestTemplate());

        when(caseDefinitionRepository.getCaseType(any())).thenCallRealMethod();
        when(caseDefinitionRepository.getLatestVersion(anyString())).thenCallRealMethod();
        when(caseDefinitionRepository.getLatestVersionFromDefinitionStore(anyString())).thenCallRealMethod();
        when(caseDefinitionRepository.getCaseType(anyInt(), anyString())).thenCallRealMethod();
        when(caseDefinitionRepository.getCaseTypesForJurisdiction(any())).thenCallRealMethod();
        when(caseDefinitionRepository.getBaseTypes()).thenReturn(Arrays.asList(fieldTypeDefinitions));
        when(caseDefinitionRepository.getUserRoleClassifications(any())).thenCallRealMethod();
        when(caseDefinitionRepository.getClassificationsForUserRoleList(any())).thenCallRealMethod();
        when(caseDefinitionRepository.getJurisdiction(anyString())).thenCallRealMethod();
        when(caseDefinitionRepository.getJurisdictionFromDefinitionStore(anyString())).thenCallRealMethod();
        return caseDefinitionRepository;
    }

    @Bean
    DataSource dataSource() throws IOException, SQLException {
        pg = postgresUtil.embeddedPostgres();
        return postgresUtil.dataSource(pg);
    }

    @PreDestroy
    void contextDestroyed() throws IOException {
        postgresUtil.contextDestroyed(pg);
    }

    @Bean
    @Primary
    UIDService uidService() {
        return Mockito.mock(UIDService.class);
    }
}
