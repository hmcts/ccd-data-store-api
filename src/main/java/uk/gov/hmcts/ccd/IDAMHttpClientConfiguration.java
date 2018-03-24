package uk.gov.hmcts.ccd;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
class IDAMHttpClientConfiguration {

    private HttpClient idamHttpClient;

    public IDAMHttpClientConfiguration(@Value("${auth.log.unsuccessful.auth.details:true}") boolean logUnsuccessfulAuthDetails) {
        this.idamHttpClient = new LoggingHTTPClient(logUnsuccessfulAuthDetails);
    }

    @Bean
    public HttpClient serviceTokenGeneratorHttpClient() {
        return idamHttpClient;
    }

    @Bean
    public HttpClient serviceTokenParserHttpClient() {
        return idamHttpClient;
    }

    @Bean
    public HttpClient userTokenParserHttpClient() {
        return idamHttpClient;
    }

    private static class LoggingHTTPClient implements HttpClient {

        private static final HttpClient WRAPPED_CLIENT = HttpClients.createDefault();

        private static final Logger LOG = LoggerFactory.getLogger(LoggingHTTPClient.class);

        private boolean logBadTokenResponse;

        public LoggingHTTPClient(boolean logBadTokenResponse) {
            this.logBadTokenResponse = logBadTokenResponse;
        }

        @Override
        @SuppressWarnings("squid:CallToDeprecatedMethod")
        public HttpParams getParams() {
            return WRAPPED_CLIENT.getParams();
        }

        @Override
        @SuppressWarnings("squid:CallToDeprecatedMethod")
        public ClientConnectionManager getConnectionManager() {
            return WRAPPED_CLIENT.getConnectionManager();
        }

        @Override
        public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException {
            try {
                return logResponseAndReturn(WRAPPED_CLIENT.execute(httpUriRequest), httpUriRequest);
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException {
            try {
                return logResponseAndReturn(WRAPPED_CLIENT.execute(httpUriRequest, httpContext), httpUriRequest);
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException {
            try {
                return logResponseAndReturn(WRAPPED_CLIENT.execute(httpHost, httpRequest), httpRequest);
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException {
            try {
                return logResponseAndReturn(WRAPPED_CLIENT.execute(httpHost, httpRequest, httpContext), httpRequest);
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException {
            try {
                return WRAPPED_CLIENT.execute(httpUriRequest, loggingResponseHandler(responseHandler, httpUriRequest));
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException {
            try {
                return WRAPPED_CLIENT.execute(httpUriRequest, loggingResponseHandler(responseHandler, httpUriRequest), httpContext);
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException {
            try {
                return WRAPPED_CLIENT.execute(httpHost, httpRequest, loggingResponseHandler(responseHandler, httpRequest));
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        @Override
        public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException {
            try {
                return WRAPPED_CLIENT.execute(httpHost, httpRequest, loggingResponseHandler(responseHandler, httpRequest), httpContext);
            } catch (IOException e) {
                logIdamError(e);
                throw e;
            }
        }

        private HttpResponse logResponseAndReturn(HttpResponse response, HttpRequest httpRequest) {
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    LOG.debug("200 response from IDAM");
                    break;
                case 401:
                    if (logBadTokenResponse) {
                        logBadTokenResponse(httpRequest, response);
                    }
                    break;
                default:
                    logErrorMessage(httpRequest, response);
            }
            return response;
        }

        private String detailedLogMessage(String message, HttpRequest httpRequest, HttpResponse response) throws IOException {
            return String.format("%s\nHeaders\n----------%s----------\n%sResponse Body\n----------%s----------\n",
                message,
                Arrays.asList(httpRequest.getAllHeaders())
                    .stream()
                    .map(header -> String.format("%s : %s\n", header.getName(), header.getValue()))
                    .collect(Collectors.joining()),
                httpRequest instanceof HttpPost ?
                    String.format(
                        "Request Body \n-------\n%s-------\n ",
                        EntityUtils.toString(((HttpPost) httpRequest).getEntity())
                    ) : "",
                EntityUtils.toString(response.getEntity())
            );
        }

        private void logIdamError(Throwable e) {
            LOG.error("An error occurred calling IDAM.", e);
        }

        private void logBadTokenResponse(HttpRequest httpRequest, HttpResponse response) {
            String message = String.format(
                "'Invalid User Token' response from IDAM for request to %s",
                httpRequest.getRequestLine().getUri()
            );
            try {
                LOG.info(detailedLogMessage(message, httpRequest, response));
            }
            catch (Exception e) {
                LOG.info(String.format("%s. Exception occurred when creating detailed message", message), e);
            }
        }

        private void logErrorMessage(HttpRequest httpRequest, HttpResponse response) {
            String message = String.format(
                "Non-200 status of %s from IDAM for %s request to %s.",
                response.getStatusLine(),
                httpRequest.getRequestLine().getMethod(),
                httpRequest.getRequestLine().getUri()
            );
            try {
                LOG.error(detailedLogMessage(message, httpRequest, response));
            } catch (Exception e) {
                LOG.error(String.format("%s. Exception occurred when creating detailed message", message), e);
            }
        }

        private <T> ResponseHandler<T> loggingResponseHandler(ResponseHandler<? extends T> responseHandler, HttpRequest httpRequest) {
            return (HttpResponse httpResponse)
                -> responseHandler.handleResponse(logResponseAndReturn(httpResponse, httpRequest));
        }

    }

}
