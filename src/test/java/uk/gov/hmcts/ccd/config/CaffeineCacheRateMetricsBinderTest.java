package uk.gov.hmcts.ccd.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CaffeineCacheRateMetricsBinder")
class CaffeineCacheRateMetricsBinderTest {

    @Test
    @DisplayName("should register both per-cache and overall metrics")
    void shouldRegisterPerCacheAndOverallMetrics() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder().recordStats());
        cacheManager.setCacheNames(List.of("cacheOne", "cacheTwo"));

        Cache cacheOne = cacheManager.getCache("cacheOne");
        Cache cacheTwo = cacheManager.getCache("cacheTwo");

        cacheOne.put("k1", "v1");
        cacheOne.get("k1");
        cacheOne.get("missing");

        cacheTwo.put("k2", "v2");
        cacheTwo.get("k2");
        cacheTwo.get("missing");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new CaffeineCacheRateMetricsBinder(cacheManager).bindTo(registry);

        double hitsCacheOne = registry.get("cache.hits").tag("cache", "cacheOne").functionCounter().count();
        double hitsCacheTwo = registry.get("cache.hits").tag("cache", "cacheTwo").functionCounter().count();
        double hitsOverall = untaggedCounterValue(registry, "cache.hits");

        double missesCacheOne = registry.get("cache.misses").tag("cache", "cacheOne").functionCounter().count();
        double missesCacheTwo = registry.get("cache.misses").tag("cache", "cacheTwo").functionCounter().count();
        double missesOverall = untaggedCounterValue(registry, "cache.misses");

        double putsCacheOne = registry.get("cache.puts").tag("cache", "cacheOne").functionCounter().count();
        double putsCacheTwo = registry.get("cache.puts").tag("cache", "cacheTwo").functionCounter().count();
        double putsOverall = untaggedCounterValue(registry, "cache.puts");

        double requestsOverall = untaggedCounterValue(registry, "cache.request.count");
        double hitRateOverall = untaggedGaugeValue(registry, "cache.hits.rate");

        assertAll(
            () -> assertEquals(hitsCacheOne + hitsCacheTwo, hitsOverall, 0.0001D),
            () -> assertEquals(missesCacheOne + missesCacheTwo, missesOverall, 0.0001D),
            () -> assertEquals(putsCacheOne + putsCacheTwo, putsOverall, 0.0001D),
            () -> assertEquals(hitsOverall / requestsOverall, hitRateOverall, 0.0001D)
        );
    }

    private double untaggedCounterValue(SimpleMeterRegistry registry, String meterName) {
        return registry.getMeters().stream()
            .filter(m -> meterName.equals(m.getId().getName()) && m.getId().getTags().isEmpty())
            .findFirst()
            .map(m -> ((FunctionCounter) m).count())
            .orElseThrow();
    }

    private double untaggedGaugeValue(SimpleMeterRegistry registry, String meterName) {
        return registry.getMeters().stream()
            .filter(m -> meterName.equals(m.getId().getName()) && m.getId().getTags().isEmpty())
            .findFirst()
            .map(m -> ((Gauge) m).value())
            .orElseThrow();
    }
}

