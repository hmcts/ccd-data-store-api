package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.params.provider.Arguments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public abstract class TestFixtures {
    private static final Integer VERSION_NUMBER = 1;

    protected static final String JURISDICTION_ID = "SSCS";
    protected static final String CASE_REFERENCE = "1234123412341236";
    protected static final Long REFERENCE = Long.valueOf(CASE_REFERENCE);
    protected static final String CASE_TYPE_ID = "Claim";
    protected static final String STATE = "CreatedState";
    protected static final String POST_STATE = "Updated";

    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_A = List.of(
        new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu"),
        new Tuple2<>("http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d")
    );
    protected static final List<Tuple2<String, String>> DOCUMENT_HASH_PAIR_B = List.of(
        new Tuple2<>("http://dm-store:8080/documents/ed9e0976-c001-47d7-bfeb-3dab8da17150",
            "60c9d24b6690276886tytu36fc7aa586a54bffc2982ed490c4503f4aca875b71"),
        new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)
    );
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

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @SuppressWarnings("unused")
    protected static Stream<Arguments> provideNullListParameters() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(emptyList(), null),
            Arguments.of(null, emptyList())
        );
    }

    protected static Map<String, JsonNode> fromFileAsMap(final String filename) throws IOException {
        final InputStream inputStream = getInputStream(filename);
        final TypeReference<Map<String, JsonNode>> typeReference = new TypeReference<>() {
        };
        return OBJECT_MAPPER.readValue(inputStream, typeReference);
    }

    protected static List<JsonNode> fromFileAsList(final String filename) throws IOException {
        final InputStream inputStream = getInputStream(filename);
        final TypeReference<List<JsonNode>> typeReference = new TypeReference<>() {
        };
        return OBJECT_MAPPER.readValue(inputStream, typeReference);
    }

    private static InputStream getInputStream(final String filename) {
        return TestFixtures.class.getClassLoader()
            .getResourceAsStream("tests/".concat(filename));
    }

    protected CaseDetails buildCaseDetails(final Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setReference(REFERENCE);
        caseDetails.setState(STATE);
        caseDetails.setDataClassification(emptyMap());

        caseDetails.setData(data);

        return caseDetails;
    }

    protected CaseEventDefinition buildCaseEventDefinition() {
        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setPostStates(getEventPostStates(POST_STATE));
        caseEventDefinition.setPublish(Boolean.TRUE);

        return caseEventDefinition;
    }

    protected CaseTypeDefinition buildCaseTypeDefinition() {
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(JURISDICTION_ID);
        final Version version = new Version();
        version.setNumber(VERSION_NUMBER);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
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
}
