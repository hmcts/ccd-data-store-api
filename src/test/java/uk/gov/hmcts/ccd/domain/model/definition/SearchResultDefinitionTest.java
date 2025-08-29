package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;

public class SearchResultDefinitionTest {

    private SearchResultDefinition searchResultDefinition;
    private SearchResultField srf1;
    private SearchResultField srf2;
    private SearchResultField srf3;
    private SearchResultField srf4;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        searchResultDefinition = new SearchResultDefinition();

        objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    @Test
    public void workBasket_1CompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/Data-workbasket-1.json");

        SearchResultDefinition searchResultDefinition = objectMapper
            .readValue(fileContent, SearchResultDefinition.class);
        SearchResultDefinition copiedSearchResultDefinition = searchResultDefinition.createCopy();

        assertNotEquals(searchResultDefinition.hashCode(), copiedSearchResultDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(searchResultDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedSearchResultDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void workBasket_2CompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/Data-workbasket-2.json");

        SearchResultDefinition searchResultDefinition = objectMapper
            .readValue(fileContent, SearchResultDefinition.class);
        SearchResultDefinition copiedSearchResultDefinition = searchResultDefinition.createCopy();

        assertNotEquals(searchResultDefinition.hashCode(), copiedSearchResultDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(searchResultDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedSearchResultDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void workBasketEmptyAndNullFieldInListCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/Data-workbasket-3.json");

        SearchResultDefinition searchResultDefinition = objectMapper
            .readValue(fileContent, SearchResultDefinition.class);
        var fieldEmpty = searchResultDefinition.getFields()[0];
        var fieldNull = searchResultDefinition.getFields()[1];

        assertNotNull(fieldEmpty);
        assertNull(fieldNull);

        SearchResultDefinition copiedSearchResultDefinition = searchResultDefinition.createCopy();
        var clonedFieldEmpty = copiedSearchResultDefinition.getFields()[0];
        var clonedFieldNull = copiedSearchResultDefinition.getFields()[1];

        assertNotNull(clonedFieldEmpty);
        assertNull(clonedFieldNull);

        assertNotEquals(fieldEmpty.hashCode(), clonedFieldEmpty.hashCode());
        assertNotEquals(searchResultDefinition.hashCode(), copiedSearchResultDefinition.hashCode());
    }

    @Test
    public void workBasketEmptyFieldsListInListCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/Data-workbasket-4.json");

        SearchResultDefinition searchResultDefinition = objectMapper
            .readValue(fileContent, SearchResultDefinition.class);
        assertEquals(0, searchResultDefinition.getFields().length);

        SearchResultDefinition copiedSearchResultDefinition = searchResultDefinition.createCopy();
        assertEquals(0, copiedSearchResultDefinition.getFields().length);

        copiedSearchResultDefinition.setFields(new SearchResultField[]{new SearchResultField()});
        assertEquals(0, searchResultDefinition.getFields().length);
        assertEquals(1, copiedSearchResultDefinition.getFields().length);

        assertNotEquals(searchResultDefinition.hashCode(), copiedSearchResultDefinition.hashCode());
    }

    @Test
    public void workBasketNullFieldsListInListCompareActualAndClone() {
        SearchResultDefinition searchResultDefinition = new SearchResultDefinition();
        searchResultDefinition.setFields(null);

        assertNull(searchResultDefinition.getFields());

        SearchResultDefinition copiedSearchResultDefinition = searchResultDefinition.createCopy();
        assertNull(copiedSearchResultDefinition.getFields());

        copiedSearchResultDefinition.setFields(new SearchResultField[]{new SearchResultField()});
        assertNull(searchResultDefinition.getFields());
        assertEquals(1, copiedSearchResultDefinition.getFields().length);

        assertNotEquals(searchResultDefinition.hashCode(), copiedSearchResultDefinition.hashCode());
    }

    @Nested
    @DisplayName("getFieldsUserRoles test")
    class SearchResultDefinitionGetFieldsUserRolesTest {

        @BeforeEach
        public void setUp() {
            srf1 = new SearchResultField();
            srf1.setCaseFieldId("caseReference");
            srf1.setLabel("SC number");
            srf1.setRole("role1");
            srf2 = new SearchResultField();
            srf2.setCaseFieldId("caseReference");
            srf2.setLabel("SC number");
            srf3 = new SearchResultField();
            srf3.setCaseFieldId("DateField");
            srf3.setLabel("Date");
            srf3.setRole("role1");
            srf4 = new SearchResultField();
            srf4.setCaseFieldId("MySchool");
            srf4.setLabel("School");
            srf4.setRole("role1");

            searchResultDefinition.setFields(new SearchResultField[]{srf1, srf2, srf3, srf4});
        }

        @Test
        public void shouldReturnFieldUserRoles() {
            Map<String, List<String>> result = searchResultDefinition.getFieldsUserRoles();

            assertAll(
                () -> assertThat(result.get(srf1.getCaseFieldId()).size(), CoreMatchers.is(2)),
                () -> assertTrue(result.get(srf1.getCaseFieldId()).contains(null)),
                () -> assertTrue(result.get(srf1.getCaseFieldId()).contains("role1")),
                () -> assertThat(result.get(srf3.getCaseFieldId()).size(), CoreMatchers.is(1)),
                () -> assertTrue(result.get(srf3.getCaseFieldId()).contains("role1")),
                () -> assertThat(result.get(srf4.getCaseFieldId()).size(), CoreMatchers.is(1)),
                () -> assertTrue(result.get(srf4.getCaseFieldId()).contains("role1"))
            );
        }

        @Test
        public void shouldReturnEmptyMapIfFieldsEmpty() {
            searchResultDefinition.setFields(new SearchResultField[]{});
            Map<String, List<String>> result = searchResultDefinition.getFieldsUserRoles();

            assertThat(result.size(), CoreMatchers.is(0));
        }
    }

    @Nested
    @DisplayName("getFieldsWithPaths test")
    class SearchResultDefinitionGetFieldsWithPathsTest {

        @BeforeEach
        public void setUp() {
            srf1 = new SearchResultField();
            srf1.setCaseFieldId("caseReference");
            srf1.setCaseFieldPath("pathToCaseReference");
            srf2 = new SearchResultField();
            srf2.setCaseFieldId("caseReference");
            srf2.setCaseFieldPath("pathToCaseReference");
            srf3 = new SearchResultField();
            srf3.setCaseFieldId("DateField");
            srf3.setCaseFieldPath("pathToDateField");
            srf4 = new SearchResultField();
            srf4.setCaseFieldId("MySchool");
            srf4.setCaseFieldPath("pathToMySchool");

            searchResultDefinition.setFields(new SearchResultField[]{srf1, srf2, srf3, srf4});
        }

        @Test
        public void shouldReturnAllFieldsWithPaths() {
            List<SearchResultField> resultFieldsWithPaths = searchResultDefinition.getFieldsWithPaths();
            assertThat(resultFieldsWithPaths.size(), CoreMatchers.is(4));
        }

        @Test
        public void shouldReturnTheFieldsWithPaths() {
            srf1.setCaseFieldPath(null);
            srf4.setCaseFieldPath("");
            searchResultDefinition.setFields(new SearchResultField[]{srf1, srf2, srf3, srf4});

            List<SearchResultField> resultFieldsWithPaths = searchResultDefinition.getFieldsWithPaths();
            assertThat(resultFieldsWithPaths.size(), CoreMatchers.is(2));
        }

        @Test
        public void shouldReturnNoFieldsWhenNoneHavePaths() {
            srf1.setCaseFieldPath(null);
            srf2.setCaseFieldPath("");
            srf3.setCaseFieldPath(null);
            srf4.setCaseFieldPath("");
            searchResultDefinition.setFields(new SearchResultField[]{srf1, srf2, srf3, srf4});

            List<SearchResultField> resultFieldsWithPaths = searchResultDefinition.getFieldsWithPaths();
            assertThat(resultFieldsWithPaths.size(), CoreMatchers.is(0));
        }
    }

    @Nested
    @DisplayName("fieldExists test")
    class SearchResultDefinitionFieldExistsTest {

        @BeforeEach
        public void setUp() {
            srf1 = new SearchResultField();
            srf1.setCaseFieldId("caseReference");
            srf1.setCaseFieldPath("pathToCaseReference");
            srf1.setRole("role1");
            srf2 = new SearchResultField();
            srf2.setCaseFieldId("caseReference");
            srf2.setCaseFieldPath("pathToCaseReference");
            srf1.setRole("role1");

            searchResultDefinition.setFields(new SearchResultField[]{srf1, srf2});
        }

        @Test
        public void shouldReturnTrueIfFieldExists() {
            assertAll(
                () -> assertTrue(searchResultDefinition.fieldExists(srf1.getCaseFieldId())),
                () -> assertTrue(searchResultDefinition.fieldExists(srf2.getCaseFieldId()))
            );
        }

        @Test
        public void shouldReturnFalseIfFieldDoesNotExists() {
            assertAll(
                () -> assertFalse(searchResultDefinition.fieldExists("random_field")),
                () -> assertFalse(searchResultDefinition.fieldExists("invalid_field"))
            );
        }

        @Test
        public void shouldReturnFalseIfCaseFieldIdIsNull() {
            assertFalse(searchResultDefinition.fieldExists(null));
        }
    }

    @Nested
    @DisplayName("fieldHasRole test")
    class SearchResultDefinitionFieldHasRoleTest {

        private Set<String> roles;

        @BeforeEach
        public void setUp() {
            srf1 = new SearchResultField();
            srf1.setCaseFieldId("caseReference");
            srf1.setLabel("SC number");
            srf1.setRole("role1");
            srf2 = new SearchResultField();
            srf2.setCaseFieldId("caseReference");
            srf2.setLabel("SC number");
            srf3 = new SearchResultField();
            srf3.setCaseFieldId("DateField");
            srf3.setLabel("Date");
            srf3.setRole("solicitor");
            srf4 = new SearchResultField();
            srf4.setCaseFieldId("MySchool");
            srf4.setLabel("School");
            srf4.setRole("judge");

            SearchResultField srf5 = new SearchResultField();
            srf5.setCaseFieldId("region");
            srf5.setLabel("Region");
            srf5.setRole("clerk");

            roles = Set.of("role1", "super-user", "solicitor");

            searchResultDefinition.setFields(new SearchResultField[]{srf1, srf2, srf3, srf4});
        }

        @Test
        public void shouldReturnTrueIfFieldHasRole() {
            assertAll(
                () -> assertTrue(searchResultDefinition.fieldHasRole("caseReference", roles)),
                () -> assertTrue(searchResultDefinition.fieldHasRole("DateField", roles))
            );
        }

        @Test void shouldReturnFalseWhenAFieldDoesNotHaveAnyOfTheRolesPassed() {
            assertAll(
                () -> assertFalse(searchResultDefinition.fieldHasRole("MySchool", roles)),
                () -> assertFalse(searchResultDefinition.fieldHasRole("region", roles))
            );
        }

        @Test
        public void shouldReturnFalseIfFieldDoesNotHaveAnyUserRoles() {
            searchResultDefinition.setFields(new SearchResultField[]{});

            assertAll(
                () -> assertFalse(searchResultDefinition.fieldHasRole("caseReference", roles)),
                () -> assertFalse(searchResultDefinition.fieldHasRole("DateField", roles)),
                () -> assertFalse(searchResultDefinition.fieldHasRole("MySchool", roles)),
                () -> assertFalse(searchResultDefinition.fieldHasRole("region", roles))
            );
        }
    }
}
