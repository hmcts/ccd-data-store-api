package uk.gov.hmcts.ccd.data;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.BUILDING_LOCATIONS_CACHE;
import static uk.gov.hmcts.ccd.data.ReferenceDataRepository.SERVICES_CACHE;

public abstract class AbstractReferenceDataIT extends WireMockBaseTest implements ReferenceDataTestFixtures {

    @Inject
    protected ReferenceDataRepository underTest;

    @BeforeEach
    void prepare() {
        List.of(BUILDING_LOCATIONS_CACHE, SERVICES_CACHE)
            .parallelStream()
            .forEach(cacheName -> underTest.invalidateCache(cacheName));

        List.of(BUILDING_LOCATIONS_STUB_ID, SERVICES_STUB_ID).forEach(id -> {
            final Optional<StubMapping> stubMapping = Optional.ofNullable(wireMockServer.getSingleStubMapping(id));
            stubMapping.ifPresent(mapping -> wireMockServer.removeStub(mapping));
        });

        wireMockServer.resetAll();
    }
}
