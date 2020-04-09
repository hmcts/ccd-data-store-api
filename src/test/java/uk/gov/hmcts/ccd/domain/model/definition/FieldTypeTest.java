package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;

import java.util.List;
import java.util.Optional;

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

        private final String COMPLEX_FIELD_TYPE = "Complex";
        private final String COLLECTION_FIELD_TYPE = "Collection";

        @Test
        void shouldFindBasicNestedField() {
            String testPath = "NestedField";
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get(), is(nestedField))
            );
        }

        @Test
        void shouldFindDeepNestedField() {
            String testPath = "NestedField.DeepNestedField";
            CaseField deepNestedField = newCaseField().withId("DeepNestedField").build();
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType(COMPLEX_FIELD_TYPE)
                    .withComplexField(newCaseField().withId("SomeOtherField").build())
                    .withComplexField(deepNestedField)
                    .build()
                ).build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get(), is(deepNestedField))
            );
        }

        @Test
        void shouldFindNestedCollectionField() {
            String testPath = "NestedCollectionField";
            CaseField collectionField = newCaseField()
                .withId("NestedCollectionField")
                .withFieldType(aFieldType().withType(COLLECTION_FIELD_TYPE)
                    .withCollectionField(newCaseField().withId("SomeOtherField").build())
                    .build()
                ).build();
            FieldType fieldType = aFieldType().withType(COLLECTION_FIELD_TYPE).withCollectionField(collectionField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(
                () -> assertThat(result.isPresent(), is(true)),
                () -> assertThat(result.get(), is(collectionField))
            );
        }

        @Test
        void shouldNotReturnResultForNonExistentNestedField() {
            String testPath = "NestedField.NonExistentDeepNestedField";
            CaseField deepNestedField = newCaseField().withId("DeepNestedField").build();
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Complex")
                    .withComplexField(newCaseField().withId("SomeOtherField").build())
                    .withComplexField(deepNestedField)
                    .build()
                ).build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(
                () -> assertThat(result.isPresent(), is(false))
            );
        }

        @Test
        void shouldNotReturnResultForBlankPath() {
            String testPath = "";
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(() -> {
                assertThat(result.isPresent(), is(false));
            });
        }

        @Test
        void shouldNotReturnResultForPathWithNoNesting() {
            String testPath = "NonNestedPath";
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(() -> {
                assertThat(result.isPresent(), is(false));
            });
        }

        @Test
        void shouldNotReturnResultForFieldTypeWithNoChildren() {
            String testPath = "Field.ID";
            CaseField nestedField = newCaseField()
                .withId("NestedField")
                .withFieldType(aFieldType().withType("Text").build())
                .build();
            FieldType fieldType = aFieldType().withType(COMPLEX_FIELD_TYPE).withComplexField(nestedField).build();

            final Optional<CommonField> result = fieldType.getNestedField(testPath);

            assertAll(() -> {
                assertThat(result.isPresent(), is(false));
            });
        }
    }
}
