package fr.lostaria.wakeapi.core;

import fr.lostaria.wakeapi.core.exception.OvhApiException;
import fr.lostaria.wakeapi.core.exception.OvhApiExceptionCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OvhApi {

    private static final Logger log = LoggerFactory.getLogger(OvhApi.class);
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int MAX_LOG_CHARS = 4_096;

    private final String appKey;
    private final String appSecret;
    private final String consumerKey;
    private final String endpoint;

    public OvhApi(
            @Value("${ovh.applicationKey}") String appKey,
            @Value("${ovh.applicationSecret}") String appSecret,
            @Value("${ovh.consumerKey}") String consumerKey,
            @Value("${ovh.apiEndpoint}") String endpoint
    ) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.consumerKey = consumerKey;
        this.endpoint = endpoint;
    }

    private final static Map<String, String> endpoints;
    static {
        endpoints = new HashMap<>();
        endpoints.put("ovh-eu", "https://eu.api.ovh.com/1.0");
        endpoints.put("ovh-ca", "https://ca.api.ovh.com/1.0");
        endpoints.put("kimsufi-eu", "https://eu.api.kimsufi.com/1.0");
        endpoints.put("kimsufi-ca", "https://ca.api.kimsufi.com/1.0");
        endpoints.put("soyoustart-eu", "https://eu.api.soyoustart.com/1.0");
        endpoints.put("soyoustart-ca", "https://ca.api.soyoustart.com/1.0");
        endpoints.put("runabove", "https://api.runabove.com/1.0");
        endpoints.put("runabove-ca", "https://api.runabove.com/1.0");
    }

    public String get(String path) throws OvhApiException { return get(path, "", true); }
    public String get(String path, boolean needAuth) throws OvhApiException { return get(path, "", needAuth); }
    public String get(String path, String body, boolean needAuth) throws OvhApiException {
        return call("GET", body, appKey, appSecret, consumerKey, endpoint, path, needAuth);
    }
    public String put(String path, String body, boolean needAuth) throws OvhApiException {
        return call("PUT", body, appKey, appSecret, consumerKey, endpoint, path, needAuth);
    }
    public String post(String path, String body, boolean needAuth) throws OvhApiException {
        return call("POST", body, appKey, appSecret, consumerKey, endpoint, path, needAuth);
    }
    public String delete(String path, String body, boolean needAuth) throws OvhApiException {
        return call("DELETE", body, appKey, appSecret, consumerKey, endpoint, path, needAuth);
    }

    private String call(String method, String body, String appKey, String appSecret, String consumerKey, String endpoint, String path, boolean needAuth) throws OvhApiException {
        long t0 = System.nanoTime();
        String resolvedEndpoint = endpoints.getOrDefault(endpoint, endpoint);
        String urlStr = resolvedEndpoint + path;

        log.info("OVH -> {} {} (auth={})", method, urlStr, needAuth);
        if (body != null && !body.isEmpty()) {
            log.debug("OVH req body: {}", truncate(body));
        }
        log.debug("OVH headers: X-Ovh-Application={}, X-Ovh-Consumer={}, endpointAlias={}",
                mask(appKey), needAuth ? mask(consumerKey) : "n/a",
                endpoint.equals(resolvedEndpoint) ? "-" : endpoint + "→" + resolvedEndpoint);

        try {
            URL url = new URL(urlStr);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setRequestMethod(method);
            request.setReadTimeout(READ_TIMEOUT_MS);
            request.setConnectTimeout(CONNECT_TIMEOUT_MS);
            request.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            request.setRequestProperty("X-Ovh-Application", appKey);

            if (needAuth) {
                long timestamp = System.currentTimeMillis() / 1000;

                String toSign = new StringBuilder(appSecret)
                        .append("+").append(consumerKey)
                        .append("+").append(method)
                        .append("+").append(url)
                        .append("+").append(body == null ? "" : body)
                        .append("+").append(timestamp)
                        .toString();
                String signature = "$1$" + HashSHA1(toSign);

                request.setRequestProperty("X-Ovh-Consumer", consumerKey);
                request.setRequestProperty("X-Ovh-Signature", signature);
                request.setRequestProperty("X-Ovh-Timestamp", Long.toString(timestamp));
                log.debug("OVH signing: ts={}, sig=$1$*** (masked)", timestamp);
            }

            if (body != null && !body.isEmpty()) {
                request.setDoOutput(true);
                try (DataOutputStream out = new DataOutputStream(request.getOutputStream())) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }

            int responseCode = request.getResponseCode();
            boolean success = (responseCode == 200); // garde ton comportement actuel
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    success ? request.getInputStream() : request.getErrorStream(),
                    StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                long dtMs = (System.nanoTime() - t0) / 1_000_000;
                if (success) {
                    log.info("OVH <- {} {} {} ({} ms)", method, urlStr, responseCode, dtMs);
                    log.debug("OVH resp body: {}", truncate(response.toString()));
                    return response.toString();
                } else {
                    log.warn("OVH <- {} {} {} ({} ms) body: {}", method, urlStr, responseCode, dtMs, truncate(response.toString()));

                    if (responseCode == 400) {
                        throw new OvhApiException(response.toString(), OvhApiExceptionCause.BAD_PARAMETERS_ERROR);
                    } else if (responseCode == 403) {
                        throw new OvhApiException(response.toString(), OvhApiExceptionCause.AUTH_ERROR);
                    } else if (responseCode == 404) {
                        throw new OvhApiException(response.toString(), OvhApiExceptionCause.RESSOURCE_NOT_FOUND);
                    } else if (responseCode == 409) {
                        throw new OvhApiException(response.toString(), OvhApiExceptionCause.RESSOURCE_CONFLICT_ERROR);
                    } else {
                        throw new OvhApiException(response.toString(), OvhApiExceptionCause.API_ERROR);
                    }
                }
            }

        } catch (NoSuchAlgorithmException e) {
            log.error("OVH error (crypto): {}", e.getMessage());
            throw new OvhApiException(e.getMessage(), OvhApiExceptionCause.INTERNAL_ERROR);
        } catch (IOException e) {
            log.error("OVH I/O error on {} {}: {}", method, urlStr, e.getMessage());
            throw new OvhApiException(e.getMessage(), OvhApiExceptionCause.INTERNAL_ERROR);
        }
    }

    public static String HashSHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : sha1hash) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    // --- utils logs ---

    private String truncate(String s) {
        if (s == null) return "";
        if (s.length() <= MAX_LOG_CHARS) return s;
        return s.substring(0, MAX_LOG_CHARS) + " …(truncated)…";
    }

    private String mask(String s) {
        if (s == null || s.isBlank()) return "null";
        int n = s.length();
        return "*".repeat(Math.max(0, n - 4)) + s.substring(Math.max(0, n - 4));
    }
}
