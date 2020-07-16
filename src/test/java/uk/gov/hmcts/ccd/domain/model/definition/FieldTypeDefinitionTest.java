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
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType("Text");

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children, is(emptyList()));
        }

        @Test
        public void getChildrenOfCollectionType() {
            CaseFieldDefinition caseFieldDefinition1 = new CaseFieldDefinition();
            caseFieldDefinition1.setId("caseField1");
            CaseFieldDefinition caseFieldDefinition2 = new CaseFieldDefinition();
            caseFieldDefinition2.setId("caseField2");

            FieldTypeDefinition collectionFieldTypeDefinition = new FieldTypeDefinition();
            collectionFieldTypeDefinition.setComplexFields(asList(caseFieldDefinition1, caseFieldDefinition2));
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType("Collection");
            fieldTypeDefinition.setCollectionFieldTypeDefinition(collectionFieldTypeDefinition);

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children.size(), is(2));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition1.getId())));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition2.getId())));
        }

        @Test
        public void getChildrenOfInvalidCollectionType() {
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType("Collection");

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children, is(emptyList()));
        }

        @Test
        public void getChildrenOfComplexType() {
            CaseFieldDefinition caseFieldDefinition1 = new CaseFieldDefinition();
            caseFieldDefinition1.setId("caseField1");
            CaseFieldDefinition caseFieldDefinition2 = new CaseFieldDefinition();
            caseFieldDefinition2.setId("caseField2");
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType("Complex");
            fieldTypeDefinition.setComplexFields(asList(caseFieldDefinition1, caseFieldDefinition2));

            List<CaseFieldDefinition> children = fieldTypeDefinition.getChildren();

            assertThat(children.size(), is(2));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition1.getId())));
            assertTrue(children.stream().anyMatch(e -> e.getId().equals(caseFieldDefinition2.getId())));
        }
    }
}
