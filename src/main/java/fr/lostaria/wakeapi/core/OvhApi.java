package fr.lostaria.wakeapi.core;

import fr.lostaria.wakeapi.core.exception.OvhApiException;
import fr.lostaria.wakeapi.core.exception.OvhApiExceptionCause;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OvhApi {

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

    public String get(String path) throws OvhApiException {
        return get(path, "", true);
    }

    public String get(String path, boolean needAuth) throws OvhApiException {
        return get(path, "", needAuth);
    }

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

    private String call(String method, String body, String appKey, String appSecret, String consumerKey, String endpoint, String path, boolean needAuth) throws OvhApiException
    {

        try {
            String indexedEndpoint = endpoints.get(endpoint);
            endpoint = (indexedEndpoint==null)?endpoint:indexedEndpoint;

            URL url = new URL(new StringBuilder(endpoint).append(path).toString());

            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setRequestMethod(method);
            request.setReadTimeout(30000);
            request.setConnectTimeout(30000);
            request.setRequestProperty("Content-Type", "application/json");
            request.setRequestProperty("X-Ovh-Application", appKey);
            if(needAuth) {
                long timestamp = System.currentTimeMillis() / 1000;

                String toSign = new StringBuilder(appSecret)
                        .append("+")
                        .append(consumerKey)
                        .append("+")
                        .append(method)
                        .append("+")
                        .append(url)
                        .append("+")
                        .append(body)
                        .append("+")
                        .append(timestamp)
                        .toString();
                String signature = new StringBuilder("$1$").append(HashSHA1(toSign)).toString();

                request.setRequestProperty("X-Ovh-Consumer", consumerKey);
                request.setRequestProperty("X-Ovh-Signature", signature);
                request.setRequestProperty("X-Ovh-Timestamp", Long.toString(timestamp));
            }

            if(body != null && !body.isEmpty())
            {
                request.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(request.getOutputStream());
                out.writeBytes(body);
                out.flush();
                out.close();
            }


            String inputLine;
            BufferedReader in;
            int responseCode = request.getResponseCode();
            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(request.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if(responseCode == 200) {
                return response.toString();
            } else if(responseCode == 400) {
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

        } catch (NoSuchAlgorithmException e) {
            throw new OvhApiException(e.getMessage(), OvhApiExceptionCause.INTERNAL_ERROR);
        } catch (IOException e) {
            throw new OvhApiException(e.getMessage(), OvhApiExceptionCause.INTERNAL_ERROR);
        }

    }

    public static String HashSHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < sha1hash.length; i++) {
            sb.append(Integer.toString((sha1hash[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}
