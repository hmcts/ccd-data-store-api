package uk.gov.hmcts.ccd.v2.internal.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.v2.internal.resource.UserProfileViewResource;

@DisplayName("UIStartTriggerControllerTest")
class UIUserProfileControllerTest {

    private static final String caseReference = "1234123412341238";

    @Mock
    private GetUserProfileOperation getUserProfileOperation;

    @Mock
    private UserProfile userProfile;

    @InjectMocks
    private UIUserProfileController profileController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(getUserProfileOperation.execute(AccessControlService.CAN_READ)).thenReturn(userProfile);
    }

    @Nested
    @DisplayName("GET /internal/profile")
    class GetProfile {

        @Test
        @DisplayName("should return 200 when profile found")
        void caseFound() {
            final ResponseEntity<UserProfileViewResource> response = profileController.getUserProfile();

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getUserProfile(), is(userProfile))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getUserProfileOperation.execute(AccessControlService.CAN_READ)).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class,
                () -> profileController.getUserProfile());
        }
    }

}
