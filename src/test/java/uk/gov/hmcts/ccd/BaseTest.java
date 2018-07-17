package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.client.DocumentManagementRestClient;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public abstract class BaseTest {
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected static final TypeReference STRING_NODE_TYPE = new TypeReference<HashMap<String, JsonNode>>() {};
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

    @Before
    public void initMock() throws IOException {

        // IDAM
        final SecurityUtils securityUtils = mock(SecurityUtils.class);
        Mockito.when(securityUtils.authorizationHeaders()).thenReturn( new HttpHeaders());
        Mockito.when(securityUtils.userAuthorizationHeaders()).thenReturn( new HttpHeaders());
        ReflectionTestUtils.setField(caseDefinitionRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(uiDefinitionRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(userRepository, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(callbackService, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(documentManagementRestClient, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(draftGateway, "securityUtils", securityUtils);

        setupUIDService();
    }

    private void setupUIDService() {
        reset(uidService);
        when(uidService.generateUID()).thenReturn(REFERENCE);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();
    }

    @BeforeClass
    public static void init() {
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @After
    public void clearDownData() {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        jdbcTemplate.queryForList(
            "SELECT " +
                "'TRUNCATE TABLE \"' || tablename || '\" CASCADE;' as truncate_statement " +
            "FROM pg_tables " +
            "WHERE schemaname = 'public' " +
            "AND tablename NOT IN ('databasechangeloglock','databasechangelog')"
        ).stream()
            .map(resultRow -> resultRow.get("truncate_statement"))
            .forEach(truncateStatement ->
                            jdbcTemplate.execute(truncateStatement.toString())
            );

    }

    protected CaseDetails mapCaseData(ResultSet resultSet, Integer i) throws SQLException {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(resultSet.getLong("id")));
        caseDetails.setReference(resultSet.getLong("reference"));
        caseDetails.setState(resultSet.getString("state"));
        caseDetails.setSecurityClassification(SecurityClassification.valueOf(resultSet.getString("security_classification")));
        caseDetails.setCaseTypeId(resultSet.getString("case_type_id"));
        final Timestamp createdAt = resultSet.getTimestamp("created_date");
        if(null != createdAt) {
            caseDetails.setCreatedDate(createdAt.toLocalDateTime());
        }
        final Timestamp modifiedAt = resultSet.getTimestamp("last_modified");
        if (null != modifiedAt) {
            caseDetails.setLastModified(modifiedAt.toLocalDateTime());
        }
        try {
            caseDetails.setData(mapper.convertValue(
                mapper.readTree(resultSet.getString("data")),
                STRING_NODE_TYPE));
        } catch (IOException e) {
            fail("Incorrect JSON structure: " + resultSet.getString("data"));
        }
        final String dataClassification = resultSet.getString("data_classification");
        if (null != dataClassification) {
            try {
                caseDetails.setDataClassification(mapper.convertValue(
                    mapper.readTree(dataClassification),
                    new TypeReference<HashMap<String, JsonNode>>() {
                    }));
            } catch (IOException e) {
                fail("Incorrect JSON structure: " + dataClassification);
            }
        }
        return caseDetails;
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
            auditEvent.setData(mapper.convertValue(
                mapper.readTree(resultSet.getString("data")),
                STRING_NODE_TYPE));
        } catch (IOException e) {
            fail("Incorrect JSON structure: " + resultSet.getString("DATA"));
        }

        final String dataClassification = resultSet.getString("data_classification");
        if (null != dataClassification) {
            try {
                auditEvent.setDataClassification(mapper.convertValue(
                    mapper.readTree(dataClassification),
                    new TypeReference<HashMap<String, JsonNode>>() {
                    }));
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
        final Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setId(jurisdictionId);

        final CaseType caseType = new CaseType();
        caseType.setId(caseTypeId);

        final CaseEvent eventTrigger = new CaseEvent();
        eventTrigger.setId(eventId);

        return eventTokenService.generateToken(userId, getCase(template, caseReference), eventTrigger, jurisdiction, caseType);
    }

    protected String generateEventTokenNewCase(String userId, String jurisdictionId, String caseTypeId, String eventId) {
        final Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setId(jurisdictionId);

        final CaseType caseType = new CaseType();
        caseType.setId(caseTypeId);

        final CaseEvent eventTrigger = new CaseEvent();
        eventTrigger.setId(eventId);

        return eventTokenService.generateToken(userId, eventTrigger, jurisdiction, caseType);
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

    public static List<CaseField> getCaseFieldsFromJson(String json) throws IOException {
        return mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, CaseField.class));
    }
}
