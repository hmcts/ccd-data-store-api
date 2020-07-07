package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.IndexSettings;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT;

@TestConfiguration
@ConditionalOnProperty(value = "search.elastic.embedded")
public class ElasticsearchITConfiguration {

    private static final String CONFIG_DIR = "elasticsearch/config";
    private static final String INDEX_SETTINGS = "classpath:" + CONFIG_DIR + "/index-settings.json";
    public static final String INDEX_TYPE = "_doc";
    public static final String[] INDICES = { "aat_cases", "mapper_cases", "security_cases", "restricted_security_cases" };

    @Value("${search.elastic.version}")
    private String elasticVersion;
    @Value("${search.elastic.port}")
    private int httpPortValue;

    @Bean
    public EmbeddedElastic embeddedElastic() throws IOException {
        EmbeddedElastic.Builder builder = EmbeddedElastic.builder()
            .withElasticVersion(elasticVersion)
            .withSetting(HTTP_PORT, httpPortValue)
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withStartTimeout(30, TimeUnit.SECONDS);

        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        for (String idx : INDICES) {
            builder.withIndex(idx, IndexSettings.builder()
                .withType(INDEX_TYPE, resourceResolver
                    .getResource(String.format("classpath:%s/mappings-%s.json", CONFIG_DIR, idx))
                    .getInputStream())
                .withSettings(resourceResolver.getResource(INDEX_SETTINGS).getInputStream())
                .build());
        }

        return builder.build();
    }

    @PreDestroy
    public void contextDestroyed() throws IOException {
        if (embeddedElastic() != null) {
            embeddedElastic().stop();
        }
    }
}
