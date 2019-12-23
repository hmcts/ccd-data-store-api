package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

class DefaultGetBannerOperationTest {

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @InjectMocks
    private DefaultGetBannerOperation defaultGetBannerOperation;

    private List<String> jurisdictionReferences = Lists.newArrayList("TEST", "FAMILY LAW");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("return empty list of banners for the passed jurisdictions")
    void shouldReturnEmptyBannerList() {
        doReturn(createBannersResultWithEmptyCollection()).when(uiDefinitionRepository).getBanners(jurisdictionReferences);

        List<Banner> banners = defaultGetBannerOperation.execute(jurisdictionReferences);
        assertEquals(0, banners.size());
    }

    private BannersResult createBannersResultWithEmptyCollection() {
        BannersResult bannersResult = new BannersResult(Lists.emptyList());
        return bannersResult;
    }

    @Test
    @DisplayName("return list  of banners for the passed jurisdictions")
    void shouldReturnBannerList() {
        doReturn(createBannersResultWithCollection()).when(uiDefinitionRepository).getBanners(jurisdictionReferences);

        List<Banner> banners = defaultGetBannerOperation.execute(jurisdictionReferences);
        assertEquals(2, banners.size());
    }

    private BannersResult createBannersResultWithCollection() {
        List<Banner> banners = Lists.newArrayList(new Banner(), new Banner());
        BannersResult bannersResult = new BannersResult();
        bannersResult.setBanners(banners);
        return bannersResult;
    }

}
