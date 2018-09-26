package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;

class ElasticsearchQueryParserFactoryTest {

    @Mock
    private ObjectMapperService objectMapperService;

    @InjectMocks
    private ElasticsearchQueryParserFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should create query parser")
    void shouldCreateQueryParser() {
        assertThat(factory.createParser("query"), instanceOf(ElasticsearchQueryParser.class));
    }
}
