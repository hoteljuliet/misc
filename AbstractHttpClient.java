package com.comcast.mirs.sixoneone.client;

import com.comcast.mirs.sixoneone.exception.SixOneOneException;
import com.comcast.mirs.sixoneone.exception.SixOneOneExceptionType;
import com.comcast.mirs.sixoneone.service.KeystoreService;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHttpClient {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected String requestUrl;
    protected List<HttpStatus> validReturnStatuses = new ArrayList<>();
    protected Boolean proxyEnabled;

    @Autowired
    protected KeystoreService keystoreService;

    @Value("${http.proxy.host}")
    private String proxyHost;

    @Value("${http.proxy.port}")
    private Integer proxyPort;

    @Value("${http.proxy.user}")
    private String proxyUser;

    @Value("${http.proxy.pass}")
    private String proxyPassword;

    private boolean isReady = false;
    private boolean isCredentialsRequired = false;
    private CloseableHttpClient httpclient;
    private RequestConfig requestConfig;
    private DefaultProxyRoutePlanner defaultProxyRoutePlanner;
    private CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private SSLContext sslContext;
    private String encoding = "UTF-8";

    /**
     * Default Constructor
     */
    public AbstractHttpClient () {}

    public void applyRequestConfig(int connectTimeout, int connectRequestTimeout) {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectRequestTimeout)
                .build();
    }

    protected void applyRoutePlanner() {
        HttpHost httpProxyHost = new HttpHost(proxyHost, proxyPort);
        defaultProxyRoutePlanner = new DefaultProxyRoutePlanner(httpProxyHost);
    }

    private void applyProxyCredentialsProvider() {
        credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPassword));
        isCredentialsRequired = true;
    }

    public void applyCredentialsProvider(HttpHost host, String userName, String pass) {
        credentialsProvider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(userName, pass));
        isCredentialsRequired = true;
    }

    public void applySSLContext() {
        try {
            sslContext = SSLContext.getInstance("SSL");

            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } }, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            logger.error("Can't apply custom SSLContext", ex);
        }
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public boolean isReady() { return isReady; }

    /**
     * Client Build
     */
    public void clientBuild() {
        if (proxyEnabled) {
            applyRoutePlanner();
            applyProxyCredentialsProvider();
        }

        HttpClientBuilder clientBuilder = HttpClients.custom();
        if (null != requestConfig) {
            clientBuilder.setDefaultRequestConfig(requestConfig);
        }
        if (isCredentialsRequired) {
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        if (null != defaultProxyRoutePlanner) {
            clientBuilder.setRoutePlanner(defaultProxyRoutePlanner);
        }
        if (null != sslContext) {
            clientBuilder.setSSLContext(sslContext);
        }

        httpclient = clientBuilder.build();
        isReady = true;
    }

    /**
     * @return String
     */
    public String execute(HttpUriRequest httpRequest) throws SixOneOneException {
        try (InputStream inputStream = getInputStream(httpRequest)) {
            return IOUtils.toString(inputStream, encoding);
        } catch(IOException ex) {
            logger.error("Can't transform Stream to String.");
            throw new SixOneOneException("Can't transform Stream to String.", SixOneOneExceptionType.HTTP_CLIENT_INCORRECT_STREAM);
        }
    }

    /**
     * @return InputStream
     */
    public InputStream getInputStream(HttpUriRequest httpRequest) throws SixOneOneException {
        try (CloseableHttpResponse response = httpclient.execute(httpRequest)) {
            logger.info("Request to: " + httpRequest.getURI().toString());
            HttpEntity entity = checkStatusCodeAndGetHttpEntity(response);
            if (null != entity) {
                InputStream inputStream = IOUtils.toBufferedInputStream(entity.getContent());
                EntityUtils.consumeQuietly(entity);
                return inputStream;
            } else {
                logger.error("Can't get HttpEntity from response.");
                throw new SixOneOneException("Can't get HttpEntity from response.", SixOneOneExceptionType.HTTP_CLIENT_INCORRECT_HTTPENTITY);
            }
        } catch(Exception ex) {
            throw new SixOneOneException("Exception thrown while requesting " + getClass().getSimpleName(), SixOneOneExceptionType.HTTP_CLIENT_EXECUTE_FAILURE);
        }
    }

    private HttpEntity checkStatusCodeAndGetHttpEntity(CloseableHttpResponse response) throws SixOneOneException, IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!validReturnStatuses.contains(HttpStatus.valueOf(statusCode))) {
            logger.error("Status code: " + statusCode);
            logger.error("Response Entity String: " + EntityUtils.toString(response.getEntity()));
            throw new SixOneOneException("Status code: " + statusCode, SixOneOneExceptionType.HTTP_CLIENT_WRONG_STATUS);
        } else {
            return response.getEntity();
        }
    }
}
