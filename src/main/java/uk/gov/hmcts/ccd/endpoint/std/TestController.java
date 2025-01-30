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

/*
 * To use :-
 * In build.gradle , in sonar.exclusions , add "TestController.java".
 * In SecurityConfiguration , in AUTH_WHITELIST , add "/jcdebug" and "/jcdebugtest".
 * Update LOG_URL.
 */
@RestController
public class TestController {

    private static final String LOG_URL = "https://ccd-data-store-api-pr-2522.preview.platform.hmcts.net/jcdebug";

    private static final Logger LOG = LoggerFactory.getLogger(TestController.class);

    /*
     * Method to POST and log messages.
     *
     * Tested originally using curl command below :-
     * curl -X POST https://ccd-data-store-api-pr-2356.preview.platform.hmcts.net/jcdebug -d "TESTING WORKS"
     */
    @PostMapping("/jcdebug")
    public ResponseEntity<String> postMessage(@RequestBody String message) {
        if (message != null) {
            message = message.replaceAll("[\n\r]", "_");
            LOG.debug("JCDEBUG: info: Message: " + message);
            LOG.debug("JCDEBUG: debug: Message: " + message);
        }
        return ResponseEntity.ok("Message: " + (message == null ? "NULL" : message));
    }

    /*
     * Method to TEST message logging.
     * Copy to SecurityValidationService , CallbackInvoker , (and CallbackService).
     */
    @GetMapping("/jcdebugtest")
    public String jcdebugtest() {
        return jcLog("TESTING POST");
    }

    /*
     * ==== Log message. ====
     */
    public static String jcLog(final String message) {
        String rc = "";
        try {
            URL apiUrl = new URL(LOG_URL);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");
            // Write the string payload to the HTTP request body
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            rc = "Response Code: " + connection.getResponseCode();
        } catch (Exception e) {
            rc = "EXCEPTION";
            e.printStackTrace();
        }
        return "jcLog: " + rc;
    }
}
