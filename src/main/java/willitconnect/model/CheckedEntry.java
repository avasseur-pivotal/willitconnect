package willitconnect.model;

import org.springframework.http.HttpStatus;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Holds a hostname or url that is checked to see if we can connect to it.
 */
public class CheckedEntry {
    private Date lastChecked;
    private String entry;
    private boolean canConnect;
    private int httpStatus;
    private String httpExtra = "";//ALEX
    private String httpProxy;

    public boolean isCanConnect() {
        return canConnect();
    }

    public Date getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public boolean canConnect() {
        return canConnect;
    }

    public void setCanConnect(boolean canConnect) {
        this.canConnect = canConnect;
    }

    public CheckedEntry(String entry) {
        Objects.requireNonNull(entry);
        this.lastChecked = Date.from(Instant.EPOCH);
        this.entry = entry;
        this.canConnect = false;
    }

    public boolean isValidHostname() {
        return entry.matches("[\\w\\.-]+:\\d+");
    }

    public boolean isValidUrl() {
        try {
            new URL(entry);
            return true;
        } catch (Exception e) {}
        return false;
    }

    /**
     * give back the status as an int to avoid forcing consumers to understand
     * how java marshals status codes to something other than an int.
     * @return the status
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Set the status of the request
     * @param statusCode in an HttpStatus to save ourselves validation headache
     */
    public void setHttpStatus(HttpStatus statusCode) {
        httpStatus = statusCode.value();
    }

    public void setHttpProxy(String proxy) {
        this.httpProxy = proxy;
    }

    public String getHttpProxy() {
        return httpProxy;
    }
    
    public void setHttpExtra(String extra) {
        this.httpExtra = extra;
    }

    public String getHttpExtra() {
        return httpExtra;
    }
}
