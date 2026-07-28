package uk.gov.hmcts.ccd.domain.types.sanitiser.client;

import com.xebialabs.restito.server.StubServer;
import org.junit.jupiter.api.AfterEach;

public abstract class StubServerDependent {
    protected StubServer server;

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
