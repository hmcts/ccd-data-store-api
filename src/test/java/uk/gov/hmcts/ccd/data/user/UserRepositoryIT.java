package uk.gov.hmcts.ccd.data.user;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
public class UserRepositoryIT {

    private static final String JURISDICTION_ID = "DIVORCE";

    @SpyBean
    private DefaultUserRepository userRepository;

    @Autowired
    private CachedUserRepository cachedUserRepository;

    @Test
    public void shouldCacheClassificationForSubsequentCalls() {
        final HashSet<SecurityClassification> expectedClassifications = Sets.newHashSet(PUBLIC);
        doReturn(expectedClassifications).when(userRepository).getUserClassifications(JURISDICTION_ID);

        final Set<SecurityClassification> classifications1 = cachedUserRepository
            .getUserClassifications(JURISDICTION_ID);

        assertThat(classifications1, is(expectedClassifications));
        verify(userRepository, times(1)).getUserClassifications(JURISDICTION_ID);

        final Set<SecurityClassification> classifications = cachedUserRepository.getUserClassifications(
                JURISDICTION_ID);

        assertAll(
            () -> assertThat(classifications, is(expectedClassifications)),
            () -> verifyNoMoreInteractions(userRepository)
        );
    }

    @Test
    public void shouldRetrieveUserRolesFromDecorated() {
        final HashSet<SecurityClassification> expectedClassifications = Sets.newHashSet(PRIVATE);
        doReturn(expectedClassifications).when(userRepository).getUserClassifications(JURISDICTION_ID);

        doReturn(PRIVATE).when(userRepository).getHighestUserClassification(JURISDICTION_ID);

        SecurityClassification classification1 = cachedUserRepository.getHighestUserClassification(JURISDICTION_ID);

        assertAll(
            () -> assertThat(classification1, is(PRIVATE)),
            () -> verify(userRepository, times(1)).getUserClassifications(JURISDICTION_ID)
        );

        SecurityClassification classification2 = cachedUserRepository.getHighestUserClassification(JURISDICTION_ID);

        assertAll(
            () -> assertThat(classification2, is(PRIVATE)),
            () -> verifyNoMoreInteractions(userRepository)
        );
    }
}
