package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseRoleRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.DocumentsOperation;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.client.DocumentManagementRestClient;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
public abstract class BaseTest {
    protected static final ObjectMapper mapper = JacksonUtils.MAPPER;
    protected static final Slf4jNotifier slf4jNotifier = new Slf4jNotifier(true);

    protected static final MediaType JSON_CONTENT_TYPE = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        Charset.forName("utf8"));

    protected static final String REFERENCE = "1504259907353529";

    protected final CallbackResponse wizardStructureResponse = new CallbackResponse();

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    protected DataSource db;
    @Inject
    protected UIDService uidService;
    @Inject
    @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
    private CaseDefinitionRepository caseDefinitionRepository;
    @Inject
    @Qualifier(DefaultCaseRoleRepository.QUALIFIER)
    private CaseRoleRepository caseRoleRepository;
    @Inject
    private HttpUIDefinitionGateway uiDefinitionRepository;
    @Inject
    @Qualifier(DefaultUserRepository.QUALIFIER)
    private UserRepository userRepository;
    @Inject
    @Qualifier(DefaultDraftGateway.QUALIFIER)
    private DraftGateway draftGateway;
    @Inject
    private CallbackService callbackService;
    @Inject
    private EventTokenService eventTokenService;
    @Inject
    private DocumentManagementRestClient documentManagementRestClient;
    @Inject
    private DocumentsOperation documentsOperation;
    @Inject
    protected SecurityUtils securityUtils;

    @Mock
    protected Authentication authentication;
    @Mock
    protected SecurityContext securityContext;

    @Before
    @BeforeEach
    public void initMock() throws IOException {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(caseRoleRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(caseDefinitionRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(uiDefinitionRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(userRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(callbackService, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(documentManagementRestClient, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(draftGateway, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(documentsOperation, "securityUtils", securityUtils);

        // Reset static field `caseDefinitionRepository`
        ReflectionTestUtils.setField(BaseType.class, "caseDefinitionRepository", caseDefinitionRepository);

        setupUIDService();

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_TEST_PUBLIC);
    }

    private void setupUIDService() {
        reset(uidService);
        when(uidService.generateUID()).thenReturn(REFERENCE);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();
    }

    @BeforeClass
    @BeforeAll
    public static void init() {
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Force re-initialisation of base types for each test suite
        ReflectionTestUtils.setField(BaseType.class, "initialised", false);
    }

    @After
    @AfterEach
    public void clearDownData() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        List<String> tables = determineTables(jdbcTemplate);
        List<String> sequences = determineSequences(jdbcTemplate);

        String truncateTablesQuery =
            "START TRANSACTION;\n" +
                tables.stream()
                    .map(record -> String.format("TRUNCATE TABLE %s CASCADE;", record))
                    .collect(Collectors.joining("\n")) +
                "\nCOMMIT;";
        jdbcTemplate.execute(truncateTablesQuery);

        sequences.forEach(sequence -> jdbcTemplate.execute("ALTER SEQUENCE " + sequence + " RESTART WITH 1"));
    }

    private List<String> determineTables(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("SELECT * FROM pg_catalog.pg_tables").stream()
            .filter(tableInfo -> tableInfo.get("schemaname").equals("public"))
            .map(tableInfo -> (String)tableInfo.get("tablename"))
            .filter(BaseTest::notLiquibase)
            .collect(Collectors.toList());
    }

    private static boolean notLiquibase(String tableName) {
        return !tableName.equals("databasechangelog") && !tableName.equals("databasechangeloglock");
    }

    private List<String> determineSequences(JdbcTemplate jdbcTemplate) {
        final String sequenceNameKey = "relname";
        String query = "SELECT c.relname FROM pg_class c WHERE c.relkind = 'S'";
        return jdbcTemplate.queryForList(query).stream()
            .map(sequenceInfo -> (String) sequenceInfo.get(sequenceNameKey))
            .collect(Collectors.toList());
    }

    protected CaseDetails mapCaseData(ResultSet resultSet, Integer i) throws SQLException {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(resultSet.getLong("id")));
        caseDetails.setReference(resultSet.getLong("reference"));
        caseDetails.setState(resultSet.getString("state"));
        caseDetails.setSecurityClassification(SecurityClassification.valueOf(resultSet.getString("security_classification")));
        caseDetails.setCaseTypeId(resultSet.getString("case_type_id"));
        final Timestamp createdAt = resultSet.getTimestamp("created_date");
        if (null != createdAt) {
            caseDetails.setCreatedDate(createdAt.toLocalDateTime());
        }
        final Timestamp modifiedAt = resultSet.getTimestamp("last_modified");
        if (null != modifiedAt) {
            caseDetails.setLastModified(modifiedAt.toLocalDateTime());
        }
        final Timestamp lastStateModified = resultSet.getTimestamp(CaseDetailsEntity.LAST_STATE_MODIFIED_DATE_FIELD_COL);
        if (null != lastStateModified) {
            caseDetails.setLastStateModifiedDate(lastStateModified.toLocalDateTime());
        }
        try {
            caseDetails.setData(JacksonUtils.convertValue(mapper.readTree(resultSet.getString("data"))));
        } catch (IOException e) {
            fail("Incorrect JSON structure: " + resultSet.getString("data"));
        }
        final String dataClassification = resultSet.getString("data_classification");
        if (null != dataClassification) {
            try {
                caseDetails.setDataClassification(JacksonUtils.convertValue(mapper.readTree(dataClassification)));
            } catch (IOException e) {
                fail("Incorrect JSON structure: " + dataClassification);
            }
        }
        caseDetails.setVersion(resultSet.getInt("version"));
        return caseDetails;
    }

    protected SignificantItem mapSignificantItem(ResultSet resultSet, Integer i) throws SQLException {
        final SignificantItem significantItem = new SignificantItem();

        significantItem.setType(resultSet.getString("type"));
        significantItem.setDescription(resultSet.getString("description"));
        significantItem.setUrl(resultSet.getString("URL"));
        return significantItem;
    }

    protected AuditEvent mapAuditEvent(ResultSet resultSet, Integer i) throws SQLException {
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId(resultSet.getLong("id"));
        auditEvent.setUserId(resultSet.getString("user_id"));
        auditEvent.setUserFirstName(resultSet.getString("user_first_name"));
        auditEvent.setUserLastName(resultSet.getString("user_last_name"));
        auditEvent.setCaseDataId(String.valueOf(resultSet.getLong("case_data_id")));
        final Timestamp createdAt = resultSet.getTimestamp("created_date");
        if (null != createdAt) {
            auditEvent.setCreatedDate(createdAt.toLocalDateTime());
        }
        auditEvent.setCaseTypeId(resultSet.getString("case_type_id"));
        auditEvent.setCaseTypeVersion(resultSet.getInt("case_type_version"));
        auditEvent.setStateId(resultSet.getString("state_id"));
        auditEvent.setStateName(resultSet.getString("state_name"));
        auditEvent.setEventId(resultSet.getString("event_id"));
        auditEvent.setEventName(resultSet.getString("event_name"));
        auditEvent.setDescription(resultSet.getString("description"));
        auditEvent.setSummary(resultSet.getString("summary"));
        auditEvent.setSecurityClassification(SecurityClassification.valueOf(resultSet.getString("security_classification")));

        try {
            auditEvent.setData(JacksonUtils.convertValue(mapper.readTree(resultSet.getString("data"))));
        } catch (IOException e) {
            fail("Incorrect JSON structure: " + resultSet.getString("DATA"));
        }

        final String dataClassification = resultSet.getString("data_classification");
        if (null != dataClassification) {
            try {
                auditEvent.setDataClassification(JacksonUtils.convertValue(mapper.readTree(dataClassification)));
            } catch (IOException e) {
                fail("Incorrect JSON structure: " + dataClassification);
            }
        }

        return auditEvent;
    }

    protected String generateEventToken(JdbcTemplate template, String userId, String jurisdictionId, String caseTypeId, String caseReference, String eventId) {
        return generateEventToken(template, userId, jurisdictionId, caseTypeId, new Long(caseReference), eventId);
    }

    protected String generateEventToken(JdbcTemplate template, String userId, String jurisdictionId, String caseTypeId, Long caseReference, String eventId) {
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(jurisdictionId);

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(caseTypeId);

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId(eventId);

        return eventTokenService.generateToken(userId, getCase(template, caseReference), caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);
    }

    protected String generateEventTokenNewCase(String userId, String jurisdictionId, String caseTypeId, String eventId) {
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(jurisdictionId);

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(caseTypeId);

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId(eventId);

        return eventTokenService.generateToken(userId, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);
    }

    protected CaseDetails getCase(JdbcTemplate template, String caseReference) {
        return getCase(template, new Long(caseReference));
    }

    protected CaseDetails getCase(JdbcTemplate template, Long caseReference) {
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        return caseDetailsList.stream()
            .filter(caseDetails -> caseReference.equals(caseDetails.getReference()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Could not find case with reference: " + caseReference));
    }

    public static String getResourceAsString(String absoluteFilename) {
        return new Scanner(BaseTest.class.getResourceAsStream(absoluteFilename), "UTF-8")
            .useDelimiter("\\A")
            .next();
    }

    public static List<CaseFieldDefinition> getCaseFieldsFromJson(String json) throws IOException {
        return mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, CaseFieldDefinition.class));
    }
}
