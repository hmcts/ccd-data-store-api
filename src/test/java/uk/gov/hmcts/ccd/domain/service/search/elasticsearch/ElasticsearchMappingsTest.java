package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class ElasticsearchMappingsTest {

    private ElasticsearchMappings elasticsearchMappings;

    @BeforeEach
    void setUp() {
        ElasticsearchMappings.TypeMappings typeMappings = new ElasticsearchMappings.TypeMappings(
            Arrays.asList("Text", "TextArea", "FixedList", "FixedListEdit", "MultiSelectList", "FixedRadioList", "DynamicList"),
            Arrays.asList("Number", "MoneyGBP"),
            Arrays.asList("Date", "Time", "DateTime"),
            Arrays.asList("PhoneUK"),
            Arrays.asList("YesOrNo", "Email", "Postcode"),
            Arrays.asList("Document")
        );

        ElasticsearchMappings.CasePredefinedMappings casePredefinedMappings = new ElasticsearchMappings.CasePredefinedMappings(
            Arrays.asList("reference", "jurisdiction", "state", "case_type_id"),
            Arrays.asList("id"),
            Arrays.asList("created_date", "last_modified"),
            Arrays.asList("security_classification"),
            Arrays.asList("@timestamp", "@version", "index_id")
        );

        elasticsearchMappings = new ElasticsearchMappings(typeMappings, casePredefinedMappings);
    }

    @Nested
    public class IsDefaultTextCaseDataTest {

        @Test
        void shouldReturnTrueWhenCaseDataFieldTypeIsMappedAsDefaultText() {
            FieldTypeDefinition fieldType = aFieldType().withType("FixedList").build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertTrue(result);
        }

        @Test
        void shouldReturnTrueWhenCaseDataFieldCollectionTypeIsMappedAsDefaultText() {
            FieldTypeDefinition fieldType = aFieldType().withType("Collection").withCollectionFieldType(
                aFieldType().withType("Text").build()
            ).build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenCaseDataFieldTypeIsNotMappedAsDefaultText() {
            FieldTypeDefinition fieldType = aFieldType().withType("Date").build();

            final boolean result = elasticsearchMappings.isDefaultTextCaseData(fieldType);

            assertFalse(result);
        }

        @Test
        void shouldReturnFalseWhenCaseDataFieldCollectionTypeIsNotMappedAsDefaultText() {
            FieldTypeDefinition fieldType = aFieldType().withType("Collection").withCollectionFieldType(
                aFieldType().withType("Number").build()
            ).build();

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
