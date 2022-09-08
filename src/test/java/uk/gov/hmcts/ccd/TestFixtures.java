package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.params.provider.Arguments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public abstract class TestFixtures {
    protected static final Integer VERSION_NUMBER = 1;
    protected static final String JURISDICTION_ID = "SSCS";
    protected static final String JURISDICTION_NAME = "Social Security and Child Support";
    protected static final String CASE_REFERENCE = "1234123412341236";
    protected static final Long REFERENCE = Long.valueOf(CASE_REFERENCE);
    protected static final String CASE_TYPE_ID = "Claim";
    protected static final String CASE_TYPE_NAME = "Claim Case Type";
    protected static final String STATE = "CreatedState";
    protected static final String POST_STATE = "Updated";

    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_A = List.of(
        new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu"),
        new Tuple2<>("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d")
    );

    protected static final List<Tuple2<String, String>> DOCUMENT_NO_HASH_PAIR_A = List.of(
        new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976", null),
        new Tuple2<>("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c", null)
    );

    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_B = List.of(
        new Tuple2<>("http://dm-store:8080/documents/ed9e0976-c001-47d7-bfeb-3dab8da17150",
            "60c9d24b6690276886tytu36fc7aa586a54bffc2982ed490c4503f4aca875b71"),
        new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)
    );

    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_C = List.of(
        new Tuple2<>("http://dm-store:8080/documents/ed9e0976-c001-47d7-bfeb-3dab8da17150", null),
        new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null),
        new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu"),
        new Tuple2<>("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d")
    );

    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_PRE = List.of(
        new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976", null),
        new Tuple2<>("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c", null)
    );
    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_POST = List.of(
        new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976", null),
        new Tuple2<>("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c", null),
        new Tuple2<>("http://dm-store:8080/documents/ed9e0976-c001-47d7-bfeb-3dab8da17150",
            "60c9d24b6690276886tytu36fc7aa586a54bffc2982ed490c4503f4aca875b71"),
        new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d",
            "ed490c4503f4aca875b7160c9d24b6690276886tytu36fc7aa586a54bffc2982")
    );
    protected static final DocumentHashToken DOC_A1 = DocumentHashToken.builder()
        .id("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976")
        .build();
    protected static final DocumentHashToken DOC_A2 = DocumentHashToken.builder()
        .id("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c")
        .build();
    protected static final DocumentHashToken HASH_TOKEN_A1 = DocumentHashToken.builder()
        .id("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976")
        .hashToken("36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu")
        .build();
    protected static final DocumentHashToken HASH_TOKEN_A2 = DocumentHashToken.builder()
        .id("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c")
        .hashToken("36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d")
        .build();
    protected static final DocumentHashToken HASH_TOKEN_B1 = DocumentHashToken.builder()
        .id("http://dm-store:8080/documents/ed9e0976-c001-47d7-bfeb-3dab8da17150")
        .hashToken("60c9d24b6690276886tytu36fc7aa586a54bffc2982ed490c4503f4aca875b71")
        .build();
    protected static final DocumentHashToken HASH_TOKEN_B2 = DocumentHashToken.builder()
        .id("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d")
        .build();

    protected final BuildingLocation location1 = BuildingLocation.builder()
        .epimmsId("321")
        .buildingLocationId("BL-1")
        .buildingLocationName("Location 1")
        .regionId("R-1")
        .region("Region 1")
        .build();
    protected final BuildingLocation location2 = BuildingLocation.builder()
        .epimmsId("L-2")
        .buildingLocationId("BL-2")
        .buildingLocationName("Location 2")
        .regionId("123")
        .region("Region 2")
        .build();

    protected final ServiceReferenceData service1 = ServiceReferenceData.builder()
        .serviceCode("SC1")
        .serviceShortDescription("Service 1")
        .build();
    protected final ServiceReferenceData service2 = ServiceReferenceData.builder()
        .serviceCode("SC2")
        .serviceShortDescription("Service 2")
        .build();

    protected final List<BuildingLocation> locationsRefData = List.of(location1, location2);
    protected final List<ServiceReferenceData> servicesRefData = List.of(service1, service2);

    protected static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @SuppressWarnings("unused")
    protected static Stream<Arguments> provideNullListParameters() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(emptyList(), null),
            Arguments.of(null, emptyList())
        );
    }

    // TODO: consider consolidating this and `loadCaseDataFromJson(...)` below
    protected static Map<String, JsonNode> fromFileAsMap(final String filename) throws IOException {
        final InputStream inputStream = getTestsInputStream(filename);
        final TypeReference<Map<String, JsonNode>> typeReference = new TypeReference<>() {
        };
        return mapper.readValue(inputStream, typeReference);
    }

    @SuppressWarnings("SameParameterValue")
    protected static List<JsonNode> fromFileAsList(final String filename) throws IOException {
        final InputStream inputStream = getTestsInputStream(filename);
        final TypeReference<List<JsonNode>> typeReference = new TypeReference<>() {
        };
        return mapper.readValue(inputStream, typeReference);
    }

    public static String fromFileAsString(final String filePath) {
        StringBuilder json = new StringBuilder();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(getInputStream(filePath));
             final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                json.append(str);
            }
        } catch (IOException e) {
            throw new RuntimeException("Caught exception reading resource " + filePath, e);
        }
        return json.toString();
    }

    private static InputStream getInputStream(final String filename) {
        return TestFixtures.class.getClassLoader()
            .getResourceAsStream(filename);
    }

    private static InputStream getTestsInputStream(final String filename) {
        return getInputStream("tests/".concat(filename));
    }

    public static CaseDetails buildCaseDetails(final Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setReference(REFERENCE);
        caseDetails.setState(STATE);
        caseDetails.setDataClassification(emptyMap());
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setVersion(VERSION_NUMBER);

        caseDetails.setData(data);

        return caseDetails;
    }

    protected CaseEventDefinition buildCaseEventDefinition() {
        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setPostStates(getEventPostStates(POST_STATE));
        caseEventDefinition.setPublish(Boolean.TRUE);

        return caseEventDefinition;
    }

    protected JurisdictionDefinition buildJurisdictionDefinition() {
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(JURISDICTION_ID);
        jurisdictionDefinition.setName(JURISDICTION_NAME);

        return jurisdictionDefinition;
    }

    protected CaseTypeDefinition buildCaseTypeDefinition() {
        final JurisdictionDefinition jurisdictionDefinition = buildJurisdictionDefinition();
        final Version version = new Version();
        version.setNumber(VERSION_NUMBER);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
        caseTypeDefinition.setName(CASE_TYPE_NAME);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        caseTypeDefinition.setVersion(version);

        return caseTypeDefinition;
    }

    private List<EventPostStateDefinition> getEventPostStates(String... postStateReferences) {
        List<EventPostStateDefinition> postStates = new ArrayList<>();
        int i = 0;
        for (String reference : postStateReferences) {
            EventPostStateDefinition definition = new EventPostStateDefinition();
            definition.setPostStateReference(reference);
            definition.setPriority(++i);
            postStates.add(definition);
        }
        return postStates;
    }

    public static <T> T loadFromJson(final String filePath, final Class<T> valueType) throws IOException {
        final InputStream inputStream = getInputStream(filePath);
        return mapper.readValue(inputStream, valueType);
    }

    public static List<CaseFieldDefinition> getCaseFieldsFromJson(final String filePath) throws IOException {
        final InputStream inputStream = getInputStream(filePath);
        return mapper.readValue(inputStream, TypeFactory.defaultInstance()
            .constructCollectionType(List.class, CaseFieldDefinition.class));
    }

    public static CaseDetails loadCaseDetails(final String filePath) throws IOException {
        return loadFromJson(filePath, CaseDetails.class);
    }

    public static CaseFieldDefinition loadCaseFieldFromJson(final String filePath) throws IOException {
        return loadFromJson(filePath, CaseFieldDefinition.class);
    }

    public static JsonNode loadJsonNodeFromJson(final String filePath) throws IOException {
        return loadFromJson(filePath, JsonNode.class);
    }

    @SneakyThrows
    public static CaseTypeDefinition loadCaseTypeDefinition(final String jsonString) {
        final InputStream inputStream = getInputStream(jsonString);
        final JsonNode node = mapper.readTree(inputStream)
            .at("/response/jsonBody");

        return mapper.treeToValue(node, CaseTypeDefinition.class);
    }

    public static CaseTypeDefinition loadCaseTypeDefinitionFromJson(final String filePath) throws IOException {
        return loadFromJson(filePath, CaseTypeDefinition.class);
    }

    public static List<FieldTypeDefinition> getFieldTypesFromJson(final String filePath) throws IOException {
        final InputStream inputStream = getInputStream(filePath);
        return mapper.readValue(inputStream, TypeFactory.defaultInstance()
            .constructCollectionType(List.class, FieldTypeDefinition.class));
    }

    public static Map<String, JsonNode> loadCaseDataFromJson(final String filePath) throws IOException {
        final InputStream inputStream = getInputStream(filePath);
        return mapper.readValue(inputStream, TypeFactory.defaultInstance()
            .constructMapType(Map.class, String.class, JsonNode.class));
    }

    public static Map<String, JsonNode> caseDataFromJsonString(final String filePath) throws IOException {
        return mapper.readValue(filePath, TypeFactory.defaultInstance()
            .constructMapType(Map.class, String.class, JsonNode.class));
    }
}
