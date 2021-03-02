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
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;

class DefaultGetJurisdictionUiConfigDefinitionOperationTest {

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
    void shouldReturnEmptyJurisdictionUiConfigList() {
        doReturn(createConfigResultWithEmptyCollection()).when(uiDefinitionRepository)
            .getJurisdictionUiConfigs(jurisdictionReferences);

        List<JurisdictionUiConfigDefinition> configs =
            defaultGetJurisdictionUiConfigOperation.execute(jurisdictionReferences);
        assertEquals(0, configs.size());
    }

    @Test
    @DisplayName("return list of jurisdiction UI configs for the passed jurisdictions")
    void shouldReturnJurisdictionUiConfigList() {
        doReturn(createConfigResultWithCollection()).when(uiDefinitionRepository)
            .getJurisdictionUiConfigs(jurisdictionReferences);

        List<JurisdictionUiConfigDefinition> configs =
            defaultGetJurisdictionUiConfigOperation.execute(jurisdictionReferences);
        assertEquals(2, configs.size());
    }

    private JurisdictionUiConfigResult createConfigResultWithEmptyCollection() {
        JurisdictionUiConfigResult jurisdictionUiConfigResult = new JurisdictionUiConfigResult(Lists.emptyList());
        return jurisdictionUiConfigResult;
    }

    private JurisdictionUiConfigResult createConfigResultWithCollection() {
        List<JurisdictionUiConfigDefinition> configs = Lists.newArrayList(new JurisdictionUiConfigDefinition(),
            new JurisdictionUiConfigDefinition());
        JurisdictionUiConfigResult jurisdictionUiConfigResult = new JurisdictionUiConfigResult();
        jurisdictionUiConfigResult.setConfigs(configs);
        return jurisdictionUiConfigResult;
    }

}
