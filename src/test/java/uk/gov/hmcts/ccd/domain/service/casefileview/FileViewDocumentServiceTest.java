package uk.gov.hmcts.ccd.domain.service.casefileview;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.Tuple2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseDataFromJson;

class FileViewDocumentServiceTest {
    private static Map<String, JsonNode> caseData;

    private final FileViewDocumentService underTest = new FileViewDocumentService();

    @BeforeAll
    static void prepare() throws Exception {
        caseData = loadCaseDataFromJson("tests/CaseDataExtractorDocumentData.json");
    }

    @ParameterizedTest
    @CsvSource({"state,$.state", "nationalityProof.documentEvidence,$.nationalityProof.documentEvidence",
        "extraDocUploadList.0,$.extraDocUploadList[0].value",
        "state.0.partyDetail.1.type,$.state[0].value.partyDetail[1].value.type"})
    void testShouldTransformToDocumentValueJsonPath(final String input, final String expectedJsonPath) {
        final String actualResult = FileViewDocumentService.DOCUMENT_VALUE_NODE_FUNCTION.apply(input);

        assertThat(actualResult)
            .isNotNull()
            .isEqualTo(expectedJsonPath);
    }

    @Test
    void testShouldReturnIndicesCorrectly() {
        final List<Tuple2<Integer, Integer>> result = FileViewDocumentService.DOCUMENT_ID_NODE_FUNCTION
            .apply("state[0].value.partyDetail[1].value.type");

        assertThat(result)
            .isNotEmpty()
            .hasSize(2)
            .hasSameElementsAs(List.of(new Tuple2<>(5, 9), new Tuple2<>(26, 30)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"state[0].partyDetail[1].value.type", "state[0].value.partyDetail.value.type"})
    void testShouldRaiseExceptionWhenIndicesCountDoNotMatch(final String input) {
        final Throwable thrown = catchThrowable(() -> FileViewDocumentService.DOCUMENT_ID_NODE_FUNCTION.apply(input));

        assertThat(thrown)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Invalid document path: " + input);
    }

    @ParameterizedTest
    @MethodSource("provideNullParameters")
    void testShouldRaiseExceptionWhenInputIsNull(final String input, final Map<String, JsonNode> inputCaseData) {
        final Throwable thrown = catchThrowable(() -> underTest.getDocumentNode(input, inputCaseData));

        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("provideDocumentParameters")
    void testShouldGetDocumentNode(final String input,
                                 final String expectedPath,
                                 final Map<String, String> documentNode) {
        final Tuple2<String, Map<String, String>> actualResult = underTest.getDocumentNode(input, caseData);

        assertThat(actualResult)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x._1).isNotNull().isEqualTo(expectedPath);
                assertThat(x._2).isNotNull().isEqualTo(documentNode);
            });
    }

    @ParameterizedTest
    @MethodSource("provideDocumentParametersWithMultipleNestedComplexPaths")
    void testShouldGetDocumentNodeFromMultipleNestedComplexPaths(final String input,
                                    final String expectedPath,
                                    final Map<String, String> documentNode) {
        final Tuple2<String, Map<String, String>> actualResult = underTest.getDocumentNode(input, caseData);

        assertThat(actualResult)
                .isNotNull()
                .satisfies(x -> {
                    assertThat(x._1).isNotNull().isEqualTo(expectedPath);
                    assertThat(x._2).isNotNull().isEqualTo(documentNode);
                });
    }

    private static Stream<Arguments> provideDocumentParametersWithMultipleNestedComplexPaths() {
        return Stream.of(
                Arguments.of("caseBundles.0.partyDetail.0.documents.0.type",
                        "caseBundles[efc3f6bb-7e8c-4313-9433-a5e6a186f079]"
                                + ".partyDetail[737ff730-90bb-4949-96e5-ac177ff71a54]"
                                + ".documents[2c003bf7-e879-4a79-9a50-a24de3c71047].type",
                        Map.of(
                                "document_url", "http://dm-store:8080//documents/1d1f4521-3ab7-4257-a887-340d021c6de8",
                                "document_filename", "test-a5.pdf",
                                "document_binary_url", "http://dm-store:8080//documents/1d1f4521-3ab7-4257-a887-340d021c6de8/binary"
                        )
                ),
                Arguments.of("caseBundles.0.partyDetail.1.documents.0.partyDocuments.0.type",
                        "caseBundles[efc3f6bb-7e8c-4313-9433-a5e6a186f079]"
                                + ".partyDetail[bab28064-e781-4c49-9261-f63d56c4311b]"
                                + ".documents[a9248472-82f2-4ccf-baa3-cee344ca1be2]"
                                + ".partyDocuments[20ea77f0-c588-4fb5-86fe-16984449c73b].type",
                        Map.of(
                                "document_url", "http://dm-store:8080//documents/dd1088dc-a2e1-4e2a-bea5-53f0596a808c",
                                "document_filename", "E_402 evidence.pdf",
                                "document_binary_url", "http://dm-store:8080//documents/dd1088dc-a2e1-4e2a-bea5-53f0596a808c/binary"
                        )
                ),
                Arguments.of("caseBundles.0.partyDetail.0.documents.1.supportDocuments.0.extra.0.type",
                        "caseBundles[efc3f6bb-7e8c-4313-9433-a5e6a186f079]"
                                + ".partyDetail[737ff730-90bb-4949-96e5-ac177ff71a54]"
                                + ".documents[a2398363-51af-4ba8-bdfd-d00bee5a3560]"
                                + ".supportDocuments[20ea77f0-c588-4fb5-86fe-16984449c73b]"
                                + ".extra[263dd0f2-20fb-41aa-8b01-6f8453aa86eb].type",
                        Map.of(
                                "document_url", "http://dm-store:8080//documents/c1c12494-73f5-4c7a-a92d-afdd4e254917",
                                "document_filename", "test-a5-document.pdf",
                                "document_binary_url", "http://dm-store:8080//documents/c1c12494-73f5-4c7a-a92d-afdd4e254917/binary"
                        )
                )
        );
    }

    protected static Stream<Arguments> provideNullParameters() {
        return Stream.of(
            Arguments.of(null, emptyMap()),
            Arguments.of("evidence.type", null),
            Arguments.of(null, null)
        );
    }

    private static Stream<Arguments> provideDocumentParameters() {
        return Stream.of(
            Arguments.of("draftOrderDoc", "draftOrderDoc",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8",
                    "document_filename", "A_Simple Document.docx",
                    "document_binary_url", "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8/binary"
                )
            ),
            Arguments.of("evidence.type", "evidence.type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea",
                    "document_filename", "B_Document Inside Complex Type.docx",
                    "document_binary_url", "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea/binary"
                )
            ),
            Arguments.of("extraDocUploadList.0", "extraDocUploadList[90a2df83-f256-43ec-aaa0-48e127a44402]",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb",
                    "document_filename", "C_1_Collection of Documents.docx",
                    "document_binary_url", "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb/binary"
                )
            ),
            Arguments.of("extraDocUploadList.1", "extraDocUploadList[84e22baf-5bec-4eec-a31f-7a3954efc9c3]",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/f6d623f2-db67-4a01-ae6e-3b6ee14a8b20",
                    "document_filename", "C_2_Collection of Documents.docx",
                    "document_binary_url", "http://dm-store:8080/documents/f6d623f2-db67-4a01-ae6e-3b6ee14a8b20/binary"
                )
            ),
            Arguments.of("state.0.partyDetail.0.type",
                "state[01827252-e41c-476a-aec0-29ef33fdb8f9]"
                    + ".partyDetail[bf0c087a-cee6-4e6e-b91c-b06a5b4c2e1f].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
                    "document_filename", "E_1_AA_Claimant details.xlsx",
                    "document_binary_url", "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary"
                )
            ),
            Arguments.of("state.0.partyDetail.1.type",
                "state[01827252-e41c-476a-aec0-29ef33fdb8f9]"
                    + ".partyDetail[6dc61824-49f8-413e-b61f-a399b9c8436b].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca",
                    "document_filename", "E_1_BBClaimant details.xlsx",
                    "document_binary_url", "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca/binary"
                )
            ),
            Arguments.of("state.1.partyDetail.0.type",
                "state[c7327e34-48b1-4b5f-8663-5bad84964fb9]"
                    + ".partyDetail[f63ebcbc-9abf-40f9-9546-8da5375de65c].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c",
                    "document_filename", "E_2_AA_Claimant details.xlsx",
                    "document_binary_url", "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c/binary"
                )
            ),
            Arguments.of("state.1.partyDetail.1.type",
                "state[c7327e34-48b1-4b5f-8663-5bad84964fb9]"
                    + ".partyDetail[81998007-6fe6-4e1e-97ab-cd8e8fb05090].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
                    "document_filename", "E_2_BB_Claimant details.xlsx",
                    "document_binary_url", "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary"
                )
            ),
            Arguments.of("state.2.partyDetail.0.type",
                "state[afa735d8-686d-40ed-b43d-0bd875b771b1]"
                    + ".partyDetail[45c8633f-6298-4f4f-97df-016e16a18d35].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09",
                    "document_filename", "E_3_AA_Claimant details.pdf",
                    "document_binary_url", "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09/binary"
                )
            ),
            Arguments.of("state.2.partyDetail.1.type",
                "state[afa735d8-686d-40ed-b43d-0bd875b771b1]"
                    + ".partyDetail[958df3a9-9a57-4717-99ed-63a7cfc3e0e2].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00",
                    "document_filename", "E_3_BB_Claimant details.pdf",
                    "document_binary_url", "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00/binary"
                )
            ),
            Arguments.of("state.2.partyDetail.2.type",
                "state[afa735d8-686d-40ed-b43d-0bd875b771b1]"
                    + ".partyDetail[930d2fe8-c0fa-4cde-9e81-6fa39ddf4441].type",
                Map.of(
                    "document_url", "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
                    "document_filename", "E_3_CC_Claimant details.pdf",
                    "document_binary_url", "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary"
                )
            )
        );
    }

}
