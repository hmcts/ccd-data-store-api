package uk.gov.hmcts.ccd.domain.service.search.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

class UserAccessFilterTest {

    @Mock
    private CaseAccessService caseAccessService;

    @InjectMocks
    private UserAccessFilter userAccessFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should return granted case ids for restricted roles")
    void shouldReturnGrantedCaseIds() {
        List<Long> caseIds = Arrays.asList(1L, 2L);
        when(caseAccessService.getGrantedCaseIdsForRestrictedRoles()).thenReturn(Optional.of(caseIds));

        Optional<List<Long>> result = userAccessFilter.getGrantedCaseIdsForRestrictedRoles();

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(caseIds));
        verify(caseAccessService).getGrantedCaseIdsForRestrictedRoles();
    }

}
