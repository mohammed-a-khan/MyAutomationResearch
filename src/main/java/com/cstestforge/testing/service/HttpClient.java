package com.cstestforge.testing.service;

import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP client for executing API requests using Java's native libraries
 */
public class HttpClient {

    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;
    private Proxy proxy;
    private int timeout = DEFAULT_TIMEOUT_MS;
    
    /**
     * Default constructor
     */
    public HttpClient() {
        // Use default SSL factory and hostname verifier
        this.sslSocketFactory = null;
        this.hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        this.proxy = null;
    }
    
    /**
     * Constructor with all configuration options
     * 
     * @param keyStoreFile Path to keystore file for client certificates
     * @param keyStorePassword Keystore password
     * @param trustStoreFile Path to truststore file for server certificates
     * @param trustStorePassword Truststore password
     * @param proxyHost Proxy host (null if no proxy)
     * @param proxyPort Proxy port
     * @param skipHostnameVerification Whether to skip hostname verification
     * @param skipCertificateValidation Whether to skip certificate validation
     * @param timeoutMillis Connection and read timeout in milliseconds
     * @throws Exception If there is an error configuring SSL
     */
    public HttpClient(String keyStoreFile, String keyStorePassword, 
                      String trustStoreFile, String trustStorePassword,
                      String proxyHost, int proxyPort, 
                      boolean skipHostnameVerification, 
                      boolean skipCertificateValidation,
                      int timeoutMillis) throws Exception {
        
        // Configure SSL
        configureSSL(keyStoreFile, keyStorePassword, trustStoreFile, trustStorePassword, skipCertificateValidation);
        
        // Configure hostname verification
        if (skipHostnameVerification) {
            this.hostnameVerifier = (hostname, session) -> true; // Skip hostname verification
        } else {
            this.hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        }
        
        // Configure proxy
        if (proxyHost != null && !proxyHost.isEmpty()) {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        } else {
            this.proxy = null;
        }
        
        // Set timeout
        this.timeout = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }
    
    /**
     * Execute an API request
     * 
     * @param request API request to execute
     * @return API response
     */
    public ApiResponse execute(ApiRequest request) {
        ApiResponse response = new ApiResponse();
        response.setId(UUID.randomUUID().toString());
        response.setRequestId(request.getId());
        response.setTimestamp(LocalDateTime.now());
        
        HttpURLConnection connection = null;
        long startTime = System.currentTimeMillis();
        
        try {
            // Build URL with query parameters
            URL url = buildUrl(request.getUrl(), request.getQueryParams());
            
            // Create connection
            connection = createConnection(url, request.getMethod());
            
            // Set headers
            setRequestHeaders(connection, request.getHeaders(), request.getBodyType());
            
            // Set request body for methods that support it
            if (hasRequestBody(request.getMethod())) {
                setRequestBody(connection, request.getBody(), request.getBodyType());
            }
            
            // Connect and get response code
            connection.connect();
            int responseCode = connection.getResponseCode();
            response.setStatusCode(responseCode);
            response.setStatusText(connection.getResponseMessage());
            
            // Get response headers
            Map<String, String> responseHeaders = connection.getHeaderFields().entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> String.join(", ", entry.getValue())
                ));
            response.setHeaders(responseHeaders);
            
            // Get content type and length
            String contentType = connection.getContentType();
            response.setContentType(contentType);
            int contentLength = connection.getContentLength();
            response.setContentLength(contentLength != -1 ? contentLength : 0);
            
            // Get response body
            if (responseCode >= 200 && responseCode < 300) {
                response.setBody(readResponseBody(connection.getInputStream()));
                response.setSuccessful(true);
            } else {
                // Try to read error stream if available
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    response.setBody(readResponseBody(errorStream));
                }
                response.setSuccessful(false);
                response.setErrorMessage("HTTP Error: " + responseCode + " " + connection.getResponseMessage());
            }
            
        } catch (Exception e) {
            response.setSuccessful(false);
            response.setErrorMessage("Request failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            
            // Calculate response time
            long endTime = System.currentTimeMillis();
            response.setResponseTimeMs(endTime - startTime);
        }
        
        return response;
    }
    
    /**
     * Configure SSL with custom keystore/truststore
     */
    private void configureSSL(String keyStoreFile, String keyStorePassword, 
                             String trustStoreFile, String trustStorePassword,
                             boolean skipCertificateValidation) throws Exception {
        
        if (skipCertificateValidation) {
            // Create a trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] { 
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            // Create SSL context with trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            this.sslSocketFactory = sslContext.getSocketFactory();
            return;
        }
        
        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;
        
        // Load keystore for client certificates if provided
        if (keyStoreFile != null && !keyStoreFile.isEmpty()) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream keyStoreStream = new FileInputStream(keyStoreFile)) {
                keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
            }
            
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        
        // Load truststore for server certificates if provided
        if (trustStoreFile != null && !trustStoreFile.isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream trustStoreStream = new FileInputStream(trustStoreFile)) {
                trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
            }
            
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        
        // Create SSL context with key and trust managers
        if (keyManagers != null || trustManagers != null) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            this.sslSocketFactory = sslContext.getSocketFactory();
        }
    }
    
    /**
     * Build URL with query parameters
     */
    private URL buildUrl(String baseUrl, Map<String, String> queryParams) throws MalformedURLException, UnsupportedEncodingException {
        if (queryParams == null || queryParams.isEmpty()) {
            return new URL(baseUrl);
        }
        
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            urlBuilder.append("?");
        } else if (!baseUrl.endsWith("&") && !baseUrl.endsWith("?")) {
            urlBuilder.append("&");
        }
        
        boolean first = true;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            first = false;
        }
        
        return new URL(urlBuilder.toString());
    }
    
    /**
     * Create HTTP connection with appropriate settings
     */
    private HttpURLConnection createConnection(URL url, String method) throws IOException {
        HttpURLConnection connection;
        
        // Use proxy if configured
        if (proxy != null) {
            connection = (HttpURLConnection) url.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        
        // Apply SSL configuration for HTTPS connections
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            
            if (sslSocketFactory != null) {
                httpsConnection.setSSLSocketFactory(sslSocketFactory);
            }
            
            if (hostnameVerifier != null) {
                httpsConnection.setHostnameVerifier(hostnameVerifier);
            }
        }
        
        // Set general connection properties
        connection.setRequestMethod(method);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setDoInput(true); // Allow response body
        
        // Set output to true for methods that send a body
        if (hasRequestBody(method)) {
            connection.setDoOutput(true);
        }
        
        return connection;
    }
    
    /**
     * Set request headers
     */
    private void setRequestHeaders(HttpURLConnection connection, Map<String, String> headers, String bodyType) {
        // Set Content-Type if body will be sent
        if (connection.getDoOutput() && bodyType != null) {
            switch (bodyType.toLowerCase()) {
                case "json":
                    connection.setRequestProperty("Content-Type", "application/json");
                    break;
                case "form":
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    break;
                case "xml":
                    connection.setRequestProperty("Content-Type", "application/xml");
                    break;
                case "text":
                    connection.setRequestProperty("Content-Type", "text/plain");
                    break;
                case "binary":
                    connection.setRequestProperty("Content-Type", "application/octet-stream");
                    break;
            }
        }
        
        // Set custom headers
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
    }
    
    /**
     * Set request body
     */
    private void setRequestBody(HttpURLConnection connection, String body, String bodyType) throws IOException {
        if (body == null || body.isEmpty()) {
            return;
        }
        
        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] bodyBytes;
            
            // Convert body to bytes based on content type
            if ("binary".equalsIgnoreCase(bodyType) && body.startsWith("base64:")) {
                // Handle base64-encoded binary data
                bodyBytes = Base64.getDecoder().decode(body.substring(7));
            } else {
                bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            }
            
            outputStream.write(bodyBytes);
            outputStream.flush();
        }
    }
    
    /**
     * Read response body from input stream
     */
    private String readResponseBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Check if the HTTP method can include a request body
     */
    private boolean hasRequestBody(String method) {
        String upperMethod = method.toUpperCase();
        return "POST".equals(upperMethod) || "PUT".equals(upperMethod) || 
               "PATCH".equals(upperMethod) || "DELETE".equals(upperMethod);
    }
    
    /**
     * Set connection timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT_MS;
    }
    
    /**
     * Get connection timeout in milliseconds
     */
    public int getTimeout() {
        return timeout;
    }
} 