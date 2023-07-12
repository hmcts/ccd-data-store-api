package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FieldTypeDefinitionTest {

    @Nested
    @DisplayName("getChildren test")
    class FieldTypeDefinitionGetChildrenTest {

        @Test
        public void getChildrenOfTextType() {
            FieldTypeDefinition fieldTypeDefinition = FieldTypeDefinition.builder()
                .type("Text")
                .build();

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children, is(emptyList()));
        }

        @Test
        public void getChildrenOfCollectionType() {
            CaseFieldDefinition caseFieldDefinition1 = CaseFieldDefinition.builder().id("caseField1").build();
            CaseFieldDefinition caseFieldDefinition2 = CaseFieldDefinition.builder().id("caseField2").build();

            FieldTypeDefinition collectionFieldTypeDefinition = FieldTypeDefinition.builder().build();
            collectionFieldTypeDefinition.setComplexFields(asList(caseFieldDefinition1, caseFieldDefinition2));
            FieldTypeDefinition fieldTypeDefinition = FieldTypeDefinition.builder()
                .type("Collection")
                .collectionFieldTypeDefinition(collectionFieldTypeDefinition)
                .build();

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children.size(), is(2));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition1.getId())));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition2.getId())));
        }

        @Test
        public void getChildrenOfInvalidCollectionType() {
            FieldTypeDefinition fieldTypeDefinition = FieldTypeDefinition.builder()
                .type("Collection")
                .build();

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children, is(emptyList()));
        }

        @Test
        public void getChildrenOfComplexType() {
            CaseFieldDefinition caseFieldDefinition1 = CaseFieldDefinition.builder().id("caseField1").build();
            CaseFieldDefinition caseFieldDefinition2 = CaseFieldDefinition.builder().id("caseField2").build();
            FieldTypeDefinition fieldTypeDefinition = FieldTypeDefinition.builder().type("Complex").build();
            fieldTypeDefinition.setComplexFields(asList(caseFieldDefinition1, caseFieldDefinition2));

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children.size(), is(2));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition1.getId())));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition2.getId())));
        }
    }
}
