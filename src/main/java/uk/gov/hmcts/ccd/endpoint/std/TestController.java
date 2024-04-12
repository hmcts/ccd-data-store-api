package uk.gov.hmcts.ccd.endpoint.std;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class TestController {

    private static final Logger LOG = LoggerFactory.getLogger(TestController.class);

    /**
     * Use this method to POST and log messages.
     */
    @PostMapping("/jcdebug")
    public ResponseEntity<String> postMessage(@RequestBody String message) {
        if (message != null) {
            message = message.replaceAll("[\n\r]", "_");
            LOG.debug("JCDEBUG: debug: Message: " + message);
            LOG.error("JCDEBUG: error: Message: " + message);
            LOG.warn("JCDEBUG: warn: Message: " + message);
            LOG.info("JCDEBUG: info: Message: " + message);
        }
        return ResponseEntity.ok("Message: " + (message == null ? "NULL" : message));
    }

    /**
     * Use this method to test message logging.
     */
    @GetMapping("/jcdebugtest")
    public String jcdebugtest() {
        return "jcdebugtest";
    }
}
