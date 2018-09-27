package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;

class ElasticsearchQueryParserFactoryTest {

    private final ElasticsearchQueryParserFactory factory = new ElasticsearchQueryParserFactory(mock(ObjectMapperService.class));

    @Test
    @DisplayName("should create query parser")
    void shouldCreateQueryParser() {
        assertThat(factory.createParser("query"), instanceOf(ElasticsearchQueryParser.class));
    }
}
