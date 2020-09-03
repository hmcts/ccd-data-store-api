package uk.gov.hmcts.ccd.data.user;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class IdamJurisdictionsResolverTest {

    @Mock
    private UserRepository userRepoMock;

    @InjectMocks
    private IdamJurisdictionsResolver jurisdictionsResolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldDelegateToUserRepository() {
        jurisdictionsResolver.getJurisdictions();

        verify(userRepoMock).getCaseworkerUserRolesJurisdictions();
    }
}
