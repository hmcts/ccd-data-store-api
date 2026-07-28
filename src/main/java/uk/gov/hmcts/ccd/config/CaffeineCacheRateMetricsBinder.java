package uk.gov.hmcts.ccd.config;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

@Component
public class CaffeineCacheRateMetricsBinder implements MeterBinder {

    private final CacheManager cacheManager;

    public CaffeineCacheRateMetricsBinder(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = caffeineCache.getNativeCache();

                FunctionCounter.builder("cache.hits", nativeCache, c -> c.stats().hitCount())
                    .description("The number of cache hits")
                    .tag("cache", cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.misses", nativeCache, c -> c.stats().missCount())
                    .description("The number of cache misses")
                    .tag("cache", cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.loads", nativeCache, c -> c.stats().loadCount())
                    .description("The number of cache loads")
                    .tag("cache", cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.average", nativeCache, c -> c.stats().loadCount())
                    .description("The number of cache average")
                    .tag("cache", cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.of", nativeCache, c -> c.stats().loadCount())
                    .description("The number of cache of")
                    .tag("cache", cacheName)
                    .register(registry);


                FunctionCounter.builder("cache.eviction", nativeCache, c -> c.stats().loadCount())
                    .description("The number of cache eviction")
                    .tag("cache", cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.eviction", nativeCache, c -> c.stats().loadCount())
                    .description("The number of cache eviction")
                    .tag("cache", cacheName)
                    .register(registry);

                Gauge.builder("cache.hitrate", nativeCache, c -> c.stats().hitRate())
                    .description("The cache hit rate")
                    .tag("cache", cacheName)
                    .register(registry);
            }
        });
    }
}
