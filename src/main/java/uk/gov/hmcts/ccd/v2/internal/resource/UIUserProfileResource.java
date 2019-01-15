package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.v2.internal.controller.UIUserProfileController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIUserProfileResource extends ResourceSupport {

    @JsonUnwrapped
    private UserProfile userProfile;

    public UIUserProfileResource(@NonNull UserProfile userProfile) {
        copyProperties(userProfile);

        add(linkTo(methodOn(UIUserProfileController.class).getUserProfile()).withSelfRel());
    }

    private void copyProperties(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
}
