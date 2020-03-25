package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfig;

@DisplayName("UIJurisdictionConfigResource")
class UIJurisdictionConfigResourceTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should copy null jurisdiction UI configs")
    void shouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            new UIJurisdictionConfigResource(null);
        });
    }

    @Test
    @DisplayName("should copy empty jurisdiction UI configs")
    void shouldCopyEmptyJurisdictionUIConfigList() {
        List<JurisdictionUiConfig> emptyList = Lists.emptyList();
        final UIJurisdictionConfigResource resource = new UIJurisdictionConfigResource(emptyList);
        assertAll(
            () -> assertThat(resource.getConfigs(), sameInstance(emptyList))
        );
    }

    @Test
    @DisplayName("should copy jurisdiction UI config list")
    void shouldCopyJurisdictionUIConfigList() {
        List<JurisdictionUiConfig> newArrayList = Lists.newArrayList(new JurisdictionUiConfig(), new JurisdictionUiConfig());
        final UIJurisdictionConfigResource resource = new UIJurisdictionConfigResource(newArrayList);
        assertAll(
            () -> assertThat(resource.getConfigs(), sameInstance(newArrayList))
        );
    }

}
