package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;

@Component
public class ElasticsearchQueryParserFactory {

    private final ObjectMapperService objectMapperService;

    @Autowired
    public ElasticsearchQueryParserFactory(ObjectMapperService objectMapperService) {
        this.objectMapperService = objectMapperService;
    }

    ElasticsearchQueryParser createParser(String query) {
        return new ElasticsearchQueryParser(objectMapperService, query);
    }
}
