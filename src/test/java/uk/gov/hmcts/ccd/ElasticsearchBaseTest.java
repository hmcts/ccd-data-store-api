package uk.gov.hmcts.ccd;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.hmcts.ccd.ElasticsearchITConfiguration.INDEX_TYPE;
import static uk.gov.hmcts.ccd.ElasticsearchITConfiguration.INDICES;

public abstract class ElasticsearchBaseTest extends WireMockBaseTest {

    private static final String DATA_DIR = "elasticsearch/data";

    private EmbeddedElastic embeddedElastic;

    @Autowired
    public final void setEmbeddedElastic(EmbeddedElastic embeddedElastic) {
        this.embeddedElastic = embeddedElastic;
    }

    @Before
    public void initElastic() throws IOException, InterruptedException {
        super.initMock();
        embeddedElastic.start();
        initData();
    }

    @After
    public void tearDownElastic() {
        embeddedElastic.stop();
    }

    private void initData() throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        for (String idx : INDICES) {
            Resource[] resources = resourceResolver.getResources(String.format("classpath:%s/%s/*.json", DATA_DIR, idx));
            for (Resource resource : resources) {
                String caseString = IOUtils.toString(resource.getInputStream(), UTF_8);
                embeddedElastic.index(idx, INDEX_TYPE, caseString);
            }
        }
    }
}
