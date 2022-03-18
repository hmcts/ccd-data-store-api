package uk.gov.hmcts.ccd.integrations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
public class IdamRepositoryCachingIT {

    private static final Logger LOG = LoggerFactory.getLogger(IdamRepositoryCachingIT.class);

    private static final String TEST_SYS_USERNAME = "testSysUsername";
    private static final String TEST_SYS_PASSWORD = "testSysPassword";
    private static final String TEST_SYS_ACCESS_TOKEN_ONE = "testSysAccessTokenOne";
    private static final String TEST_SYS_ACCESS_TOKEN_TWO = "testSysAccessTokenTwo";

    // Need to Spy ApplicationParams rather than Mock it as it is used during setup of other beans
    @SpyBean
    private ApplicationParams applicationParams;

    @MockBean
    private IdamClient idamClient;

    // SpyBean and MockBean annotations mean that IdamRepository bean will automatically use
    // mocked/spied ApplicationParams and IdamClient beans
    @Autowired
    private IdamRepository idamRepository;

    private AutoCloseable autoCloseable;

    @Before
    public void openMocks() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    /**
     * <p>Test cached behaviour of getDataStoreUserAccessToken method.  Caching is implemented using Spring
     * Cacheable annotation so test is performed on the actual IdamRepository bean with mocked dependencies.</p>
     *
     * <p>As the caching behaviour is controlled by Spring it is not reset between tests.  For this reason
     * all tests have to be in a single method so that state can be controlled.</p>
     */
    @Test
    public void shouldCacheGetDataStoreUserAccessTokenMethod() {

        // Set up initial expected behaviour for mock and spy beans
        when(applicationParams.getDataStoreSystemUserId()).thenReturn(TEST_SYS_USERNAME);
        when(applicationParams.getDataStoreSystemUserPassword()).thenReturn(TEST_SYS_PASSWORD);
        when(idamClient.getAccessToken(TEST_SYS_USERNAME, TEST_SYS_PASSWORD)).thenReturn(TEST_SYS_ACCESS_TOKEN_ONE);

        // Before testing getDataStoreUserAccessToken method first
        // confirm that none of the mocked methods have been called
        verify(applicationParams, never()).getDataStoreSystemUserId();
        verify(applicationParams, never()).getDataStoreSystemUserPassword();
        verify(idamClient, never()).getAccessToken(TEST_SYS_USERNAME, TEST_SYS_PASSWORD);

        // Call getDataStoreUserAccessToken method for the first time.  No cached
        // value should be available so the mocked methods should be called once.
        LOG.info("Test getDataStoreUserAccessToken - no cached value");
        checkGetDataStoreUserAccessToken(1, TEST_SYS_ACCESS_TOKEN_ONE);

        // Change value returned by mock IdamClient to prove that cached value is returned
        when(idamClient.getAccessToken(TEST_SYS_USERNAME, TEST_SYS_PASSWORD)).thenReturn(TEST_SYS_ACCESS_TOKEN_TWO);

        // Call getDataStoreUserAccessToken method for the second time.  Cached original value should be returned
        // and the mocked methods should still only have been called once.
        LOG.info("Test getDataStoreUserAccessToken - cached value");
        checkGetDataStoreUserAccessToken(1, TEST_SYS_ACCESS_TOKEN_ONE);

        // Pause long enough so that cache time limit expires - see system.user.token.cache.ttl.secs in test.properties
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            fail("Exception occurred whilst pausing to expire cache time limit: " + e.getMessage());
        }

        // Call getDataStoreUserAccessToken method for the third time.  As cache time limit has now
        // expired the mocked methods should have been called again making two times in total.
        LOG.info("Test getDataStoreUserAccessToken - cache expired");
        checkGetDataStoreUserAccessToken(2, TEST_SYS_ACCESS_TOKEN_TWO);
    }

    private void checkGetDataStoreUserAccessToken(final int expectedNumInvocations, final String expectedAccessToken) {
        String actualAccessToken;
        actualAccessToken = idamRepository.getDataStoreSystemUserAccessToken();

        verify(applicationParams, times(expectedNumInvocations)).getDataStoreSystemUserId();
        verify(applicationParams, times(expectedNumInvocations)).getDataStoreSystemUserPassword();
        verify(idamClient, times(expectedNumInvocations)).getAccessToken(TEST_SYS_USERNAME, TEST_SYS_PASSWORD);
        assertEquals("Unexpected Access Token value", expectedAccessToken, actualAccessToken);
    }

    @After
    public void releaseMocks() throws Exception {
        autoCloseable.close();
    }
}
