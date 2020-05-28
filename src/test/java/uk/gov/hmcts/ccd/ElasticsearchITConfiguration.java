package uk.gov.hmcts.ccd;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.IndexSettings;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MINUTES;
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT;

@Configuration
@ConditionalOnProperty(value = "search.elastic.embedded", havingValue = "true")
public class ElasticsearchITConfiguration {

    private static final String INDEX_TYPE = "_doc";
    private static final String CONFIG_DIR = "elasticsearch/config";
    private static final String INDEX_SETTINGS = "classpath:" + CONFIG_DIR + "/index-settings.json";
    private static final String DATA_DIR = "elasticsearch/data";
    private static final String[] INDICES = { "aat_cases", "mapper_cases" };

    @Value("${search.elastic.version}")
    private String elasticVersion;
    @Value("${search.elastic.port}")
    private int httpPortValue;

    private PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Bean
    public EmbeddedElastic embeddedElastic() throws IOException, InterruptedException {
        EmbeddedElastic.Builder builder = EmbeddedElastic.builder()
            .withElasticVersion(elasticVersion)
            .withSetting(HTTP_PORT, httpPortValue)
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withStartTimeout(2, MINUTES);

        for (String idx : INDICES) {
            builder.withIndex(idx, IndexSettings.builder()
                .withType(INDEX_TYPE, resourceResolver
                    .getResource(String.format("classpath:%s/mappings-%s.json", CONFIG_DIR, idx))
                    .getInputStream())
                .withSettings(resourceResolver.getResource(INDEX_SETTINGS).getInputStream())
                .build());
        }

        return builder.build().start();
    }

    @PostConstruct
    public void initData() throws IOException, InterruptedException {
        for (String idx : INDICES) {
            Resource[] resources = resourceResolver.getResources(String.format("classpath:%s/%s/*.json", DATA_DIR, idx));
            for (Resource resource : resources) {
                String caseString = IOUtils.toString(resource.getInputStream(), UTF_8);
                embeddedElastic().index(idx, INDEX_TYPE, caseString);
            }
        }
    }
}
