package uk.gov.hmcts.ccd.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


@DisplayName("CaffeineCacheRateMetricsBinder")
class CaffeineCacheRateMetricsBinderTest {

    public static final String CACHE_ONE = "cacheOne";
    public static final String CACHE_TWO = "cacheTwo";
    public static final String CACHE = "cache";
    private static final double DELTA = 0.000001D;

    private CaffeineCacheRateMetricsBinder classUnderTest;
    private CaffeineCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CaffeineCacheManager();
        classUnderTest = new CaffeineCacheRateMetricsBinder(cacheManager);
    }

    @Test
    @DisplayName("should register all supported metrics")
    void shouldRegisterAllMetrics() {
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(1).recordStats());
        cacheManager.setCacheNames(List.of(CACHE_ONE, CACHE_TWO));

        final CaffeineCache cacheOne = (CaffeineCache) cacheManager.getCache(CACHE_ONE);
        final CaffeineCache cacheTwo = (CaffeineCache) cacheManager.getCache(CACHE_TWO);

        cacheOne.put("k1", "v1");
        cacheOne.get("k1");
        cacheOne.get("missing");
        cacheOne.get("load", () -> "loaded");
        cacheOne.get("load");
        cacheOne.put("evict-1", "value-1");
        cacheOne.put("evict-2", "value-2");

        cacheTwo.put("k2", "v2");
        cacheTwo.get("k2");
        cacheTwo.get("missing");
        cacheTwo.get("load", () -> "loaded");
        cacheTwo.get("load");
        cacheTwo.put("evict-3", "value-3");
        cacheTwo.put("evict-4", "value-4");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        classUnderTest.bindTo(registry);

        CacheStats cacheOneStats = cacheOne.getNativeCache().stats();
        CacheStats cacheTwoStats = cacheTwo.getNativeCache().stats();

        assertAll(
            () -> assertEquals(cacheOneStats.hitCount(), metricValue(registry, "cache.hits", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.hitCount(), metricValue(registry, "cache.hits", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.hitRate(), metricValue(registry, "cache.hits.rate", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.hitRate(), metricValue(registry, "cache.hits.rate", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.missCount(), metricValue(registry, "cache.misses", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.missCount(), metricValue(registry, "cache.misses", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.missRate(), metricValue(registry, "cache.misses.rate", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.missRate(), metricValue(registry, "cache.misses.rate", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.loadCount(), metricValue(registry, "cache.load.count", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.loadCount(), metricValue(registry, "cache.load.count", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.averageLoadPenalty(),
                metricValue(registry, "cache.average.load.penalty", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.averageLoadPenalty(),
                metricValue(registry, "cache.average.load.penalty", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.evictionCount(), metricValue(registry, "cache.eviction.count", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.evictionCount(), metricValue(registry, "cache.eviction.count", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.evictionWeight(), metricValue(registry, "cache.eviction.weight", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.evictionWeight(), metricValue(registry, "cache.eviction.weight", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.requestCount(), metricValue(registry, "cache.request.count", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.requestCount(), metricValue(registry, "cache.request.count", CACHE_TWO), DELTA),
            () -> assertEquals(cacheOneStats.loadSuccessCount(), metricValue(registry, "cache.puts", CACHE_ONE), DELTA),
            () -> assertEquals(cacheTwoStats.loadSuccessCount(), metricValue(registry, "cache.puts", CACHE_TWO), DELTA),
            // Duplicate cache.hits registration should still resolve to a single meter per cache tag.
            () -> assertEquals(20, registry.getMeters().size())
        );
    }

    private double metricValue(SimpleMeterRegistry registry, String name, String cacheName) {
        return registry.get(name).tag(CACHE, cacheName).functionCounter().count();
    }
}
