package uk.gov.hmcts.ccd;

import org.junit.Before;

import java.io.IOException;

public abstract class ElasticsearchBaseTest extends WireMockBaseTest {

    @Before
    public void setUp() throws IOException {
        super.initMock();
    }
}
