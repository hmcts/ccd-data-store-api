package uk.gov.hmcts.ccd.domain.service.aggregated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfig;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;

class DefaultGetJurisdictionUiConfigOperationTest {

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @InjectMocks
    private DefaultGetJurisdictionUiConfigOperation defaultGetJurisdictionUiConfigOperation;

    private List<String> jurisdictionReferences = Lists.newArrayList("Reference 1", "Reference 2");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("return empty list of jurisdiction UI configs for the passed jurisdictions")
    void shouldReturnEmptyBannerList() {
        doReturn(createBannersResultWithEmptyCollection()).when(uiDefinitionRepository).getJurisdictionUiConfigs(jurisdictionReferences);

        List<JurisdictionUiConfig> configs = defaultGetJurisdictionUiConfigOperation.execute(jurisdictionReferences);
        assertEquals(0, configs.size());
    }

    @Test
    @DisplayName("return list  of banners for the passed jurisdictions")
    void shouldReturnBannerList() {
        doReturn(createConfigResultWithCollection()).when(uiDefinitionRepository).getJurisdictionUiConfigs(jurisdictionReferences);

        List<JurisdictionUiConfig> configs = defaultGetJurisdictionUiConfigOperation.execute(jurisdictionReferences);
        assertEquals(2, configs.size());
    }
    
    private JurisdictionUiConfigResult createBannersResultWithEmptyCollection() {
    	JurisdictionUiConfigResult bannersResult = new JurisdictionUiConfigResult(Lists.emptyList());
        return bannersResult;
    }

    private JurisdictionUiConfigResult createConfigResultWithCollection() {
        List<JurisdictionUiConfig> configs = Lists.newArrayList(new JurisdictionUiConfig(), new JurisdictionUiConfig());
        JurisdictionUiConfigResult jurisdictionUiConfigResult = new JurisdictionUiConfigResult();
        jurisdictionUiConfigResult.setConfigs(configs);
        return jurisdictionUiConfigResult;
    }

}
