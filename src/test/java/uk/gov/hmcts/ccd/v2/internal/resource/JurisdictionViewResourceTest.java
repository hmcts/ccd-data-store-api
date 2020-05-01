package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("UIJurisdictionResourceTest")
class JurisdictionViewResourceTest {

    private static final String ACCESS = "create";
    private static final String LINK_SELF = String.format("/internal/jurisdictions?access=%s", ACCESS);
    private static final JurisdictionDisplayProperties[] displayProperties = new JurisdictionDisplayProperties[1];

    @BeforeEach
    public void setup() {
        JurisdictionDisplayProperties properties = new JurisdictionDisplayProperties();
        displayProperties[0] = properties;
    }

    @Test
    @DisplayName("should copy jurisdiction display properties")
    void shouldCopySearchInputs() {
        final JurisdictionViewResource resource = new JurisdictionViewResource(displayProperties, ACCESS);

        assertAll(
            () -> assertThat(resource.getJurisdictions(), not(sameInstance(this.displayProperties)))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final JurisdictionViewResource resource = new JurisdictionViewResource(this.displayProperties, ACCESS);
        Optional<Link> link = resource.getLink("self");

        assertThat(link.get().getHref(), equalTo(LINK_SELF));
    }

}
