package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("UIBannerResource")
class BannerViewResourceTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should copy null banners")
    void shouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            new BannerViewResource(null);
        });
    }

    @Test
    @DisplayName("should copy empty banners")
    void shouldCopyEmptyBannerList() {
        List<Banner> emptyList = Lists.emptyList();
        final BannerViewResource resource = new BannerViewResource(emptyList);
        assertAll(
            () -> assertThat(resource.getBanners(), sameInstance(emptyList))
        );
    }

    @Test
    @DisplayName("should copy banner list")
    void shouldCopyBannerList() {
        List<Banner> newArrayList = Lists.newArrayList(new Banner(), new Banner());
        final BannerViewResource resource = new BannerViewResource(newArrayList);
        assertAll(
            () -> assertThat(resource.getBanners(), sameInstance(newArrayList))
        );
    }

}
