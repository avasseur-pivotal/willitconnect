package willitconnect.service;

import org.apache.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import willitconnect.model.CheckedEntry;
import willitconnect.service.util.Connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.*;
import java.sql.Date;
import java.time.Instant;

import static org.apache.log4j.Logger.getLogger;

class CustomResponseErrorHandler implements ResponseErrorHandler {

    private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

    public boolean hasError(ClientHttpResponse response) throws IOException {
        return errorHandler.hasError(response);
    }

    public void handleError(ClientHttpResponse response) throws IOException {
	getLogger(EntryChecker.class).info("ALEX error "+ response);	
    }
}

@Service
public class EntryChecker {
    private final RestTemplate restTemplate;
    private Logger log = getLogger(EntryChecker.class);

    public EntryChecker(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
    }

    public CheckedEntry check(CheckedEntry e) {
        log.info("checking " + e.getEntry());
        if (e.isValidUrl()) {
            checkUrl(e);
        } else if (e.isValidHostname()) {
            checkHostname(e);
        } else {
            log.info(e.getEntry() + " is not a valid hostname");
        }
        return e;
    }

    private void checkHostname(CheckedEntry e) {
        String hostname = getHostname(e);
        int port = getPort(e, hostname);
        if (null != e.getHttpProxy()) {
            String proxy = e.getHttpProxy().split(":")[0];
            int proxyPort = Integer.parseInt(e.getHttpProxy().split(":")[1]);

            e.setCanConnect(
                    Connection.checkProxyConnection(
                            hostname, port, proxy, proxyPort, "http"));
        } else {
            e.setCanConnect(Connection.checkConnection(hostname, port));
        }
        e.setLastChecked(Date.from(Instant.now()));
    }

    private void checkUrl(CheckedEntry e) {
        try {
            ClientHttpRequestFactory oldFactory = null;
            if ( null != e.getHttpProxy() ) {
                oldFactory = swapProxy(e);
            }

		log.info("ALEX restTemplate");

            ResponseEntity<String> resp =
                    restTemplate.getForEntity(e.getEntry(), String.class);
		
		log.info("ALEX resp");

            if ( null != oldFactory ) {
                restTemplate.setRequestFactory(oldFactory);
            }

            log.info("Status = " + resp.getStatusCode());
            e.setCanConnect(true);
            e.setHttpStatus(resp.getStatusCode());
       	    if (resp.getHeaders().getLocation() != null) e.setHttpExtra(resp.getHeaders().getLocation().toString());
        } catch (ResourceAccessException ex) {
            e.setCanConnect(false);
		log.info("ALEX error", ex);
        } catch (Throwable t) {
		e.setCanConnect(false);
		log.info("ALEX error", t);
	}
	log.info("ALEX DONE!!");
        e.setLastChecked(Date.from(Instant.now()));
    }

    private ClientHttpRequestFactory swapProxy(CheckedEntry e) {
        ClientHttpRequestFactory oldFactory = restTemplate.getRequestFactory();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
		protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
			super.prepareConnection(connection, httpMethod);
			connection.setInstanceFollowRedirects(true);
		} };

	requestFactory.setReadTimeout(2000);
        requestFactory.setConnectTimeout(2000);



        Proxy proxy= new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(
                    e.getHttpProxy().split(":")[0],
                    Integer.parseInt(e.getHttpProxy().split(":")[1]
                    )));
	log.info("Proxy found : " + e.getHttpProxy());
        log.info("Using proxy " + proxy + " for " + e.getEntry());
        requestFactory.setProxy(proxy);

	restTemplate.setRequestFactory(requestFactory);


	log.info("proxy done");
        return oldFactory;
    }

    private int getPort(CheckedEntry e, String hostname) {
        return Integer.parseInt(e.getEntry().substring(
                hostname.length() + 1,
                e.getEntry().length()));
    }

    private String getHostname(CheckedEntry e) {
        return e.getEntry().split(":")[0];
    }

}
