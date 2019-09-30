package uk.gov.hmcts.ccd.domain.service.callbacks.retrycontext;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.doReturn;

class CallbackRetryContextBuilderTest {

    @Mock
    private ApplicationParams applicationParams;

    private CallbackRetryContextBuilder callbackRetryContextBuilder;

    private List<Integer> defaultCallbackRetryIntervals = Lists.newArrayList(0, 1, 3);
    private Integer defaultCallbackTimeout = 1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(defaultCallbackRetryIntervals).when(applicationParams).getCallbackRetryIntervalsInSeconds();
        doReturn(defaultCallbackTimeout).when(applicationParams).getDefaultCallbackTimeoutInSeconds();
        callbackRetryContextBuilder = new CallbackRetryContextBuilder(applicationParams);
    }

    @Test
    void shouldDisableCallbackRetriesWithSingleValue() {

        List<CallbackRetryContext> callbackRetryContexts = callbackRetryContextBuilder.buildCallbackRetryContexts(Lists.newArrayList(0));

        Assertions.assertAll(
            () -> Assert.assertThat(callbackRetryContexts, hasSize(1)),
            () -> Assert.assertThat(callbackRetryContexts, hasItems(hasProperty("callbackRetryInterval", is(0)))),
            () -> Assert.assertThat(callbackRetryContexts, hasItems(hasProperty("callbackRetryTimeout", is(1))))
        );
    }

    @Test
    void shouldUseDefaultRetries() {

        List<CallbackRetryContext> callbackRetryContexts = callbackRetryContextBuilder.buildCallbackRetryContexts(Lists.newArrayList());

        Assertions.assertAll(
            () -> Assert.assertThat(callbackRetryContexts, hasSize(3)),
            () -> Assert.assertThat(callbackRetryContexts, contains(
                both(hasProperty("callbackRetryInterval", is(0)))
                    .and(hasProperty("callbackRetryTimeout", is(1))),
                both(hasProperty("callbackRetryInterval", is(1)))
                    .and(hasProperty("callbackRetryTimeout", is(1))),
                both(hasProperty("callbackRetryInterval", is(3)))
                    .and(hasProperty("callbackRetryTimeout", is(1)))
                ))
        );
    }

    @Test
    void shouldUseCustomRetries() {

        List<CallbackRetryContext> callbackRetryContexts = callbackRetryContextBuilder.buildCallbackRetryContexts(Lists.newArrayList(3, 6, 9, 12, 15));

        Assertions.assertAll(
            () -> Assert.assertThat(callbackRetryContexts, hasSize(5)),
            () -> Assert.assertThat(callbackRetryContexts, contains(
                both(hasProperty("callbackRetryInterval", is(0)))
                    .and(hasProperty("callbackRetryTimeout", is(3))),
                both(hasProperty("callbackRetryInterval", is(1)))
                    .and(hasProperty("callbackRetryTimeout", is(6))),
                both(hasProperty("callbackRetryInterval", is(3)))
                    .and(hasProperty("callbackRetryTimeout", is(9))),
                both(hasProperty("callbackRetryInterval", is(9)))
                    .and(hasProperty("callbackRetryTimeout", is(12))),
                both(hasProperty("callbackRetryInterval", is(27)))
                    .and(hasProperty("callbackRetryTimeout", is(15)))
                ))
        );
    }

    @Test
    void shouldUseDefaultRetriesWithSingleZero() {

        List<CallbackRetryContext> callbackRetryContexts = callbackRetryContextBuilder.buildCallbackRetryContexts(Lists.newArrayList(3, 6, 0, 12));

        Assertions.assertAll(
            () -> Assert.assertThat(callbackRetryContexts, hasSize(3)),
            () -> Assert.assertThat(callbackRetryContexts, contains(
                both(hasProperty("callbackRetryInterval", is(0)))
                    .and(hasProperty("callbackRetryTimeout", is(1))),
                both(hasProperty("callbackRetryInterval", is(1)))
                    .and(hasProperty("callbackRetryTimeout", is(1))),
                both(hasProperty("callbackRetryInterval", is(3)))
                    .and(hasProperty("callbackRetryTimeout", is(1)))
            ))
        );
    }
}
