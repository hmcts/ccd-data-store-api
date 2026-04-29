package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.util.CallbackHostPatternMatcher;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class CallbackUrlValidator {
    private static final String HTTPS_SCHEME = "https";
    private static final String HTTP_SCHEME = "http";
    // Cloud instance metadata endpoint; explicitly blocked to prevent SSRF credential exfiltration.
    @SuppressWarnings("java:S1313")
    private static final String METADATA_ENDPOINT = "169.254.169.254";

    private final ApplicationParams applicationParams;

    public CallbackUrlValidator(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    public void validateCallbackUrl(String url) {
        final URI callbackUri;
        try {
            callbackUri = new URI(url);
        } catch (URISyntaxException e) {
            throw new CallbackException("Invalid callback URL: " + sanitizeUrl(url));
        }

        final String scheme = Optional.ofNullable(callbackUri.getScheme()).orElse("").toLowerCase(Locale.UK);
        final String host = callbackUri.getHost();
        if (!StringUtils.hasLength(host)) {
            throw new CallbackException("Callback URL must include a host: " + sanitizeUrl(url));
        }
        if (StringUtils.hasLength(callbackUri.getUserInfo())) {
            throw new CallbackException("Callback URL must not include credentials: " + sanitizeUrl(url));
        }
        if (!isAllowedHost(host, applicationParams.getCallbackAllowedHosts())) {
            throw new CallbackException("Callback URL host is not allowlisted: " + host);
        }
        if (!HTTPS_SCHEME.equals(scheme)
            && !(HTTP_SCHEME.equals(scheme) && isAllowedHost(host, applicationParams.getCallbackAllowedHttpHosts()))) {
            throw new CallbackException("Callback URL scheme is not permitted: " + scheme);
        }
        if (resolvesToPrivateAddress(host) && !isAllowedHost(host, applicationParams.getCallbackAllowPrivateHosts())) {
            throw new CallbackException("Callback URL resolves to a private or local network address: " + host);
        }
    }

    public String sanitizeUrl(String url) {
        if (!StringUtils.hasLength(url)) {
            return "<empty>";
        }
        try {
            URI uri = new URI(url);
            String scheme = Optional.ofNullable(uri.getScheme()).orElse("unknown");
            String host = Optional.ofNullable(uri.getHost()).orElse("unknown-host");
            int port = uri.getPort();
            String path = Optional.ofNullable(uri.getPath()).orElse("");
            String portPart = port > -1 ? ":" + port : "";
            return scheme + "://" + host + portPart + path;
        } catch (URISyntaxException e) {
            return "<invalid-url>";
        }
    }

    private boolean isAllowedHost(String host, List<String> allowedHosts) {
        try {
            CallbackHostPatternMatcher.validateEntries(allowedHosts);
            return CallbackHostPatternMatcher.containsHost(host, allowedHosts);
        } catch (IllegalArgumentException ex) {
            throw new CallbackException(ex.getMessage());
        }
    }

    private boolean hostMatches(String host, String allowedHost) {
        return CallbackHostPatternMatcher.matches(host, allowedHost);
    }

    private boolean resolvesToPrivateAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isPrivateOrLocal(address)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new CallbackException("Unable to resolve callback host: " + host);
        }
    }

    private boolean isPrivateOrLocal(InetAddress address) {
        if (address instanceof Inet6Address) {
            final byte[] addressBytes = address.getAddress();
            if (addressBytes.length > 0 && (addressBytes[0] & (byte) 0xFE) == (byte) 0xFC) {
                return true; // IPv6 unique local addresses fc00::/7, including fd00::/8
            }
        }
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()
            || METADATA_ENDPOINT.equals(address.getHostAddress());
    }
}
