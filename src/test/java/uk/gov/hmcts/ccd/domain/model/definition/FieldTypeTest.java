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
}
