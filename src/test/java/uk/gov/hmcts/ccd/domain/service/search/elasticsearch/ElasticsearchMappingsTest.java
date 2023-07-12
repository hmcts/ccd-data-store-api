package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElasticsearchMappingsTest {

    private ElasticsearchMappings elasticsearchMappings;

    @BeforeEach
    void setUp() {
        ElasticsearchMappings.TypeMappings typeMappings = new ElasticsearchMappings.TypeMappings(
            Arrays.asList("Text", "TextArea", "FixedList", "FixedListEdit", "MultiSelectList", "FixedRadioList",
                    "DynamicList", "DynamicRadioList", "DynamicMultiSelectList")
        );

        ElasticsearchMappings.CasePredefinedMappings casePredefinedMappings =
                new ElasticsearchMappings.CasePredefinedMappings(
            Arrays.asList("reference", "jurisdiction", "state", "case_type_id")
        );

        elasticsearchMappings = new ElasticsearchMappings(typeMappings, casePredefinedMappings);
    }

    @Nested
    public class IsDefaultTextCaseDataTest {

        @Test
        void shouldReturnTrueWhenCaseDataFieldTypeIsMappedAsDefaultText() {
            FieldTypeDefinition fieldType = FieldTypeDefinition.builder().type("FixedList").build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertTrue(result);
        }

        @Test
        void shouldReturnTrueWhenCaseDataFieldCollectionTypeIsMappedAsDefaultText() {
            FieldTypeDefinition fieldType = FieldTypeDefinition.builder().type("Collection")
                .collectionFieldTypeDefinition(
                    FieldTypeDefinition.builder().type("Text").build()
                ).build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenCaseDataFieldTypeIsNotMappedAsDefaultText() {
            FieldTypeDefinition fieldType = FieldTypeDefinition.builder().type("Date").build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertFalse(result);
        }

        @Test
        void shouldReturnFalseWhenCaseDataFieldCollectionTypeIsNotMappedAsDefaultText() {
            FieldTypeDefinition fieldType = FieldTypeDefinition.builder().type("Collection")
                .collectionFieldTypeDefinition(FieldTypeDefinition.builder().type("Number").build())
                .build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertFalse(result);
        }
    }

    @Nested
    public class IsDefaultTextMetadataTest {

        @Test
        void shouldReturnTrueWhenMetadataIsMappedAsDefaultText() {
            final boolean result = elasticsearchMappings.isDefaultTextMetadata("state");

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenMetadataIsNotMappedAsDefaultText() {
            final boolean result = elasticsearchMappings.isDefaultTextMetadata("created_date");

            assertFalse(result);
        }
    }

}
