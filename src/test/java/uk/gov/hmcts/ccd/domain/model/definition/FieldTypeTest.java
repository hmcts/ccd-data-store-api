package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

public class FieldTypeTest {

    @Nested
    @DisplayName("getChildren test")
    class FieldTypeGetChildrenTest {

        @Test
        public void getChildrenOfTextType() {
            FieldType fieldType = new FieldType();
            fieldType.setType("Text");

            List<CaseField> children = fieldType.getChildren();

            assertThat(children, is(emptyList()));
        }

        @Test
        public void getChildrenOfCollectionType() {
            CaseField caseField1 = new CaseField();
            caseField1.setId("caseField1");
            CaseField caseField2 = new CaseField();
            caseField2.setId("caseField2");

            FieldType collectionFieldType = new FieldType();
            collectionFieldType.setComplexFields(asList(caseField1, caseField2));
            FieldType fieldType = new FieldType();
            fieldType.setType("Collection");
            fieldType.setCollectionFieldType(collectionFieldType);

            List<CaseField> children = fieldType.getChildren();

            assertThat(children.size(), is(2));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseField1.getId())));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseField2.getId())));
        }

        @Test
        public void getChildrenOfInvalidCollectionType() {
            FieldType fieldType = new FieldType();
            fieldType.setType("Collection");

            List<CaseField> children = fieldType.getChildren();

            assertThat(children, is(emptyList()));
        }

        @Test
        public void getChildrenOfComplexType() {
            CaseField caseField1 = new CaseField();
            caseField1.setId("caseField1");
            CaseField caseField2 = new CaseField();
            caseField2.setId("caseField2");
            FieldType fieldType = new FieldType();
            fieldType.setType("Complex");
            fieldType.setComplexFields(asList(caseField1, caseField2));

            List<CaseField> children = fieldType.getChildren();

            assertThat(children.size(), is(2));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseField1.getId())));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseField2.getId())));
        }
    }

    @Nested
    @DisplayName("getNestedField test")
    class FieldTypeGetNestedFieldTest {

        private final String TEXT_FIELD_TYPE = "Text";
        private final String COMPLEX_FIELD_TYPE = "Complex";
        private final String COLLECTION_FIELD_TYPE = "Collection";

        @ParameterizedTest
        @ArgumentsSource(BasicNestedFieldTestData.class)
        void shouldFindBasicNestedField(String testPath, boolean pathIncludesParent) {
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, pathIncludesParent);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get(), is(nestedField))
            );
        }

        @ParameterizedTest
        @ArgumentsSource(DeepNestedFieldTestData.class)
        void shouldFindDeepNestedField(String testPath, boolean pathIncludesParent) {
            CaseField deepNestedField = newCaseField().withId("DeepNestedField").build();
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType(COMPLEX_FIELD_TYPE)
                    .withComplexField(newCaseField().withId("SomeOtherField").build())
                    .withComplexField(deepNestedField)
                    .build()
                ).build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, pathIncludesParent);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get(), is(deepNestedField))
            );
        }

        @ParameterizedTest
        @ArgumentsSource(NestedCollectionFieldTestData.class)
        void shouldFindNestedCollectionField(String testPath, boolean pathIncludesParent) {
            CaseField collectionField = newCaseField()
                .withId("NestedCollectionField")
                .withFieldType(aFieldType().withType(COLLECTION_FIELD_TYPE)
                    .withCollectionField(newCaseField().withId("SomeOtherField").build())
                    .build()
                ).build();
            FieldType fieldType = aFieldType().withType(COLLECTION_FIELD_TYPE).withCollectionField(collectionField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, pathIncludesParent);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get(), is(collectionField))
            );
        }

        @ParameterizedTest
        @ArgumentsSource(NonExistentNestedFieldTestData.class)
        void shouldNotReturnResultForNonExistentNestedField(String testPath, boolean pathIncludesParent) {
            CaseField deepNestedField = newCaseField().withId("DeepNestedField").build();
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Complex")
                    .withComplexField(newCaseField().withId("SomeOtherField").build())
                    .withComplexField(deepNestedField)
                    .build()
                ).build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, pathIncludesParent);

            assertAll(
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void shouldNotReturnResultForBlankPath(boolean pathIncludesParent) {
            String testPath = "";
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, pathIncludesParent);

            assertAll(() -> {
                assertThat(result.isPresent(), is(false));
            });
        }

        @Test
        void shouldNotReturnResultForPathWithOnlyParent() {
            String testPath = "ParentField";
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, true);

            assertAll(() -> {
                assertThat(result.isPresent(), is(false));
            });
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void shouldNotReturnResultForFieldTypeWithNoChildren(boolean pathIncludesParent) {
            String testPath = "Field.ID";
            FieldType fieldType = aFieldType().withType(TEXT_FIELD_TYPE).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath, pathIncludesParent);

            assertAll(() -> {
                assertThat(result.isPresent(), is(false));
            });
        }
    }

    private static class BasicNestedFieldTestData implements ArgumentsProvider {
        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("NestedField", false),
                Arguments.of("Field.NestedField", true)
            );
        }
    }

    private static class DeepNestedFieldTestData implements ArgumentsProvider {
        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("NestedField.DeepNestedField", false),
                Arguments.of("Field.NestedField.DeepNestedField", true)
            );
        }
    }

    private static class NestedCollectionFieldTestData implements ArgumentsProvider {
        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("NestedCollectionField", false),
                Arguments.of("Field.NestedCollectionField", true)
            );
        }
    }

    private static class NonExistentNestedFieldTestData implements ArgumentsProvider {
        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("NestedField.NonExistentDeepNestedField", false),
                Arguments.of("Field.NestedField.NonExistentDeepNestedField", true)
            );
        }
    }
}
