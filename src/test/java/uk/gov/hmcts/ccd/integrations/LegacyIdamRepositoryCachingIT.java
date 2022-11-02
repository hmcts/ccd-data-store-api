package uk.gov.hmcts.ccd.integrations;

import org.springframework.boot.test.context.SpringBootTest;

import static uk.gov.hmcts.ccd.integrations.CachingBaseTest.LEGACY_CACHE_PROPERTIES;

// Repeat tests from superclass with legacy caching config
@SpringBootTest(properties = { LEGACY_CACHE_PROPERTIES })
public class LegacyIdamRepositoryCachingIT extends IdamRepositoryCachingIT {

}
