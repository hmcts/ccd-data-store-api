package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.MULTI_SELECT_LIST;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.NUMBER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;

class SimpleCaseTypeMetadataExtractorTest extends AbstractBaseCaseFieldMetadataExtractorTest {
    private final SimpleCaseTypeMetadataExtractor underTest = new SimpleCaseTypeMetadataExtractor();

    @Test
    @SuppressWarnings("ConstantConditions")
    void testShouldRaiseExceptionWhenInputIsNull() {
        assertThatNullPointerException().isThrownBy(() -> underTest.matches(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {TEXT, NUMBER, MULTI_SELECT_LIST, "Email", "TextArea"})
    void testShouldMatch(final String inputType) {
        final BaseType baseFieldType = BaseType.get(inputType);

        final Boolean result = underTest.matches(baseFieldType);

        assertThat(result).isNotNull().isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {COMPLEX, COLLECTION})
    void testShouldNotMatch(final String inputType) {
        final BaseType baseFieldType = BaseType.get(inputType);

        final Boolean result = underTest.matches(baseFieldType);

        assertThat(result).isNotNull().isFalse();
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
        fieldTypeDefinition.setType(BASE_FIELD_TYPE_ID);
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        final List<CaseFieldMetadata> metadataList = singletonList(new CaseFieldMetadata("timeline.0.type", null));

        final Either<CaseFieldMetadataExtractor.RecursionParams, List<CaseFieldMetadata>> results =
            underTest.extractCaseFieldData(
                new AbstractMap.SimpleEntry<>("type", dataValue),
                caseFieldDefinition,
                "timeline.0.",
                    BASE_FIELD_TYPE_ID,
                emptyList()
            );

        VavrAssertions.assertThat(results)
            .isNotNull()
            .hasRightValueSatisfying(it -> assertThat(it).hasSameElementsAs(metadataList));
    }

    @Test
    void testShouldPassWithNullBaseType() {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(BASE_FIELD_TYPE_ID);
        fieldTypeDefinition.setType(null);
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        final List<CaseFieldMetadata> metadataList = singletonList(new CaseFieldMetadata(FIELD_TYPE_ID, null));

        final Either<CaseFieldMetadataExtractor.RecursionParams, List<CaseFieldMetadata>> results =
                underTest.extractCaseFieldData(
                        nodeEntry,
                        caseFieldDefinition,
                        "",
                        BASE_FIELD_TYPE_ID,
                        emptyList()
                );
        VavrAssertions.assertThat(results)
                .isNotNull()
                .hasRightValueSatisfying(it -> assertThat(it).hasSameElementsAs(metadataList));
    }

    @ParameterizedTest
    @MethodSource("provideNonMatchingFieldTypeParameters")
    void testShouldReturnUnchangedWhenFieldTypeDoesNotMatchFieldTypeDefinitionId(final String fieldType,
                                                                                 final List<CaseFieldMetadata> paths) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(FIELD_TYPE_ID);
        fieldTypeDefinition.setType("Type-1");
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        final Either<CaseFieldMetadataExtractor.RecursionParams, List<CaseFieldMetadata>> results =
            underTest.extractCaseFieldData(
                nodeEntry,
                caseFieldDefinition,
                "",
                fieldType,
                paths
            );

        VavrAssertions.assertThat(results)
            .isNotNull()
            .hasRightValueSatisfying(it -> assertThat(it).hasSameElementsAs(paths));
    }

    @ParameterizedTest
    @MethodSource("providePreEvaluatedPathsParameters")
    void testShouldReturnUnchangedWhenFieldTypeDefinitionIsNull(final List<CaseFieldMetadata> paths) {
        final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();

        final Either<CaseFieldMetadataExtractor.RecursionParams, List<CaseFieldMetadata>> results =
            underTest.extractCaseFieldData(
                nodeEntry,
                caseFieldDefinition,
                "",
                FIELD_TYPE_ID,
                paths
            );

        VavrAssertions.assertThat(results)
            .isNotNull()
            .hasRightValueSatisfying(it -> assertThat(it).hasSameElementsAs(paths));
    }

    @ParameterizedTest
    @MethodSource("provideMatchingFieldTypeParameters")
    void testShouldEvaluatePath(final List<CaseFieldMetadata> pathsBefore, final List<CaseFieldMetadata> pathsAfter) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(FIELD_TYPE_ID_GENERATED);
        fieldTypeDefinition.setType(BASE_FIELD_TYPE_ID);
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        final Either<CaseFieldMetadataExtractor.RecursionParams, List<CaseFieldMetadata>> results =
            underTest.extractCaseFieldData(
                    nodeEntry,
                    caseFieldDefinition,
                    "",
                    BASE_FIELD_TYPE_ID,
                    pathsBefore
            );

        VavrAssertions.assertThat(results)
            .isNotNull()
            .hasRightValueSatisfying(it -> assertThat(it).hasSameElementsAs(pathsAfter));
    }

    private static Stream<Arguments> provideNonMatchingFieldTypeParameters() {
        return Stream.of(
            Arguments.of("", emptyList()),
            Arguments.of("Label", emptyList()),
            Arguments.of("", singletonList(new CaseFieldMetadata("timeline.0.type", "Cat-1"))),
            Arguments.of("Label", singletonList(new CaseFieldMetadata("timeline.0.type", "Cat-1")))
        );
    }

    private static Stream<Arguments> providePreEvaluatedPathsParameters() {
        return Stream.of(
            Arguments.of(emptyList()),
            Arguments.of(singletonList(new CaseFieldMetadata("timeline.0.type", "Cat-1")))
        );
    }

    private static Stream<Arguments> provideMatchingFieldTypeParameters() {
        return Stream.of(
            Arguments.of(emptyList(), singletonList(new CaseFieldMetadata("DocumentField", null))),
            Arguments.of(singletonList(new CaseFieldMetadata("timeline.0.type", "Cat-1")),
                List.of(new CaseFieldMetadata("timeline.0.type", "Cat-1"),
                    new CaseFieldMetadata("DocumentField", null)))
        );
    }

}
