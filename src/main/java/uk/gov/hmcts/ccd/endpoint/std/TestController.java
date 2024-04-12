package uk.gov.hmcts.ccd.endpoint.std;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    /*
     * Method to POST and log messages.
     *
     * Tested using curl command below :-
     * curl -X POST https://ccd-data-store-api-pr-2356.preview.platform.hmcts.net/jcdebug -d "TESTING WORKS"
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

    /*
     * Use this method to test message logging.
     */
    @GetMapping("/jcdebugtest")
    public String jcdebugtest() {
        String rc;

        try {
            final String url = "https://example.com/api";
            final String payload = "TESTING POST";

            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            // Set content type
            connection.setRequestProperty("Content-Type", "text/plain");

            // Write the string payload to the HTTP request body
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload.getBytes());
            outputStream.flush();
            outputStream.close();

            rc = "Response Code: " + connection.getResponseCode();
        } catch (Exception e) {
            rc = "EXCEPTION";
            e.printStackTrace();
        }
        return "jcdebugtest: " + rc;
    }
}
