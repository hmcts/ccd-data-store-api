package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class DefinitionBlockTest {

    @Test
    void shouldInitialiseDocumentTypeDefinitionTemplate() {
        Map<String, DefinitionBlock> result = DefinitionBlock.DOCUMENT_TYPE_DEFINITION;

        assertAll(
            () -> assertThat(result.size(), is(3)),
            () -> assertTextDefinition(result.get("document_url"), "document_url"),
            () -> assertTextDefinition(result.get("document_binary_url"), "document_binary_url"),
            () -> assertTextDefinition(result.get("document_filename"), "document_filename")
        );
    }

    private void assertTextDefinition(DefinitionBlock definitionBlock, String originalId) {
        assertAll(
            () -> assertThat(definitionBlock.getOriginalId(), is(originalId)),
            () -> assertThat(definitionBlock.getType(), is("SimpleText")),
            () -> assertThat(definitionBlock.getSubtype(), is("Text")),
            () -> assertThat(definitionBlock.getTypeDef(), is(nullValue()))
        );
    }
}