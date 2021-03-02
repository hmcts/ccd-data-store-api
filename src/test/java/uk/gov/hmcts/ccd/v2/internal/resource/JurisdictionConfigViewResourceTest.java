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

import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;

@DisplayName("UIJurisdictionConfigResource")
class JurisdictionConfigViewResourceTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should copy null jurisdiction UI configs")
    void shouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            new JurisdictionConfigViewResource(null);
        });
    }

    @Test
    @DisplayName("should copy empty jurisdiction UI configs")
    void shouldCopyEmptyJurisdictionUIConfigList() {
        List<JurisdictionUiConfigDefinition> emptyList = Lists.emptyList();
        final JurisdictionConfigViewResource resource = new JurisdictionConfigViewResource(emptyList);
        assertAll(
            () -> assertThat(resource.getConfigs(), sameInstance(emptyList))
        );
    }

    @Test
    @DisplayName("should copy jurisdiction UI config list")
    void shouldCopyJurisdictionUIConfigList() {
        List<JurisdictionUiConfigDefinition> newArrayList =
                Lists.newArrayList(new JurisdictionUiConfigDefinition(), new JurisdictionUiConfigDefinition());
        final JurisdictionConfigViewResource resource = new JurisdictionConfigViewResource(newArrayList);
        assertAll(
            () -> assertThat(resource.getConfigs(), sameInstance(newArrayList))
        );
    }

}
