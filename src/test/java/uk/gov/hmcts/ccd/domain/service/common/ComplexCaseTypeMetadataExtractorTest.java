package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.MULTI_SELECT_LIST;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.NUMBER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;

class ComplexCaseTypeMetadataExtractorTest extends AbstractBaseCaseFieldMetadataExtractorTest {
    private final ComplexCaseTypeMetadataExtractor underTest = new ComplexCaseTypeMetadataExtractor();

    @Test
    @SuppressWarnings("ConstantConditions")
    void testShouldRaiseExceptionWhenInputIsNull() {
        assertThatNullPointerException().isThrownBy(() -> underTest.matches(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {COLLECTION, TEXT, NUMBER, MULTI_SELECT_LIST, "Email", "TextArea"})
    void testShouldNotMatch(final String inputType) {
        final BaseType baseFieldType = BaseType.get(inputType);

        final Boolean result = underTest.matches(baseFieldType);

        assertThat(result).isNotNull().isFalse();
    }

    @Test
    void testShouldMatch() {
        final BaseType baseFieldType = BaseType.get(COMPLEX);

        final Boolean result = underTest.matches(baseFieldType);

        assertThat(result).isNotNull().isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideNullParameters")
    void testShouldRaiseExceptionWhenFieldTypeIsNull(final Map.Entry<String, JsonNode> nodeEntry,
                                                     final CaseFieldDefinition caseFieldDefinition,
                                                     final String fieldIdPrefix,
                                                     final String fieldType,
                                                     final List<CaseFieldMetadata> paths) {
        assertThatNullPointerException().isThrownBy(() -> underTest.extractCaseFieldData(
            nodeEntry,
            caseFieldDefinition,
            fieldIdPrefix,
            fieldType,
            paths
        ));
    }

    @Test
    void testShouldEvaluatePathWithFieldIdPrefix() {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(FIELD_TYPE_ID);
        fieldTypeDefinition.setType("Type-1");
        fieldTypeDefinition.setComplexFields(emptyList());
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        final CaseFieldMetadataExtractor.RecursionParams recursionParams =
            new CaseFieldMetadataExtractor.RecursionParams(
                JacksonUtils.convertValue(nodeEntry.getValue()),
                caseFieldDefinition.getFieldTypeDefinition().getComplexFields(),
                "timeline.0.",
                emptyList(),
                FIELD_TYPE_ID
            );

        final Either<CaseFieldMetadataExtractor.RecursionParams, List<CaseFieldMetadata>> results =
            underTest.extractCaseFieldData(
                new AbstractMap.SimpleEntry<>("0", dataValue),
                caseFieldDefinition,
                "timeline.",
                FIELD_TYPE_ID,
                emptyList()
            );

        VavrAssertions.assertThat(results)
            .isNotNull()
            .hasLeftValueSatisfying(it -> assertThat(it).isEqualTo(recursionParams));
    }

}
