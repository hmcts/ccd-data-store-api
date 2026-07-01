package uk.gov.hmcts.ccd.domain.types.sanitiser.client;

import com.xebialabs.restito.server.StubServer;
import com.xebialabs.restito.support.junit.ServerDependencyRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;

public abstract class StubServerDependent {
    protected StubServer server;

    @Rule
    public ServerDependencyRule serverDependency = new ServerDependencyRule();

    @BeforeEach
    public void startServer() {
        if (serverDependency.isServerDependent()) {
            server = new StubServer().run();
        }
    }

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
