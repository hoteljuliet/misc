package com.comcast.mirs.sixoneone.client;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class IOPHttpClient extends AbstractHttpClient {
    private Integer connectTimeout;
    private Integer connectRequestTimeout;

    private String createUri = "/api/now/v1/import/x_mcim_unified_case";
    private String readUri = "/api/now/table/sn_customerservice_case";

    /**
     * Constructor
     */
    @Autowired
    public IOPHttpClient (@Value("${servicenow.request.url:}") String requestUrl,
                          @Value("${servicenow.connect.timeout:2000}") int connectTimeout,
                          @Value("${servicenow.connectRequest.timeout:5000}") int connectRequestTimeout,
                          @Value("#{'${servicenow.validReturnStatuses:OK}'.split(',')}") List<String> validReturnStatuses,
                          @Value("${servicenow.proxy.enabled:false}") boolean proxyEnabled) {

        this.validReturnStatuses = validReturnStatuses.stream().map(HttpStatus::valueOf).collect(Collectors.toList());
        this.requestUrl = requestUrl;
        this.connectTimeout = connectTimeout;
        this.connectRequestTimeout = connectRequestTimeout;
        this.proxyEnabled = proxyEnabled;
    }

    /**
     * PostConstruct
     */
    @PostConstruct
    private void postConstruct() throws Exception {
        if (!requestUrl.isEmpty()) {
            Optional<UsernamePasswordCredentials> usernameAndPassword = keystoreService.getUsernameAndPassword("iop_login");

            if (usernameAndPassword.isPresent()) {
                UsernamePasswordCredentials userPassCred = usernameAndPassword.get();
                URL url = new URL(requestUrl);
                applyCredentialsProvider(new HttpHost(url.getHost()), userPassCred.getUserName(), userPassCred.getPassword());
            } else {
                logger.warn(getClass().getSimpleName() + ": Not found credentials. Please check keystore (iop_login).");
            }
            applyRequestConfig(connectTimeout, connectRequestTimeout);
            clientBuild();
        } else {
            logger.warn(getClass().getSimpleName() + ": Not initialized. Please check property.");
        }
    }

    public String createIOPTicket(String postBody) {
        try {
            HttpPost httpPost = new HttpPost(getCreateUrl());
            StringEntity stringEntity = new StringEntity(postBody);
            httpPost.setEntity(stringEntity);
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            return execute(httpPost);
        } catch (Exception ex) {
            logger.error("Exception while executing IOP Post Request", ex);
        }
        throw new RuntimeException();
    }

    public String readIOPTicket(String fieldName, String fieldValue) {
        String queryValue = fieldName + "=" + fieldValue;
        return readIOPTicket(queryValue);
    }

    public String readIOPTicket(String queryValue) {
        try {
            URI uri = new URIBuilder(getReadUrl())
                    .setParameter("sysparm_query", queryValue)
                    .setParameter("sysparm_display_value","true")
                    .build();

            HttpGet httpGet = new HttpGet(uri);

            return execute(httpGet);
        } catch (Exception ex) {
            logger.error("Exception while executing IOP Get Request", ex);
        }
        throw new RuntimeException();
    }

    public String getCreateUrl() {
        return requestUrl + createUri;
    }

    public String getReadUrl() {
        return requestUrl + readUri;
    }
}
