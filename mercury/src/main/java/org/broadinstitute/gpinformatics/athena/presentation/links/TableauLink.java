package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * This is a bean to help the UI deal with Tableau links.
 */
@Named
@RequestScoped
public class TableauLink {
    private final Logger logger = Logger.getLogger(this.getClass());
    private static final String TRUST_PATH = "/trusted/";
    private static final String PASS_REPORT_NAME = "PASS"; //the lookup key for urls in yaml config file

    private static long TRUSTED_ID_LIFETIME = 10 * 1000L;  //trusted ticket is good for at least ten seconds
    public static final String BAD_TABLEAU_TICKET = "-1";

    private String trustId = null;
    private long trustedIdTimestamp = 0L;

    @Inject
    private TableauConfig tableauConfig;

    public String serverUrl() {
        return tableauConfig.getTrustedTicketServer();
    }

    /**
     * Returns a url to the tableau report.
     * @param projectTitle
     * @return uses trusted ticketing, or normal auth path if unavailable.
     */
    public String passReportUrl(String projectTitle) {
        // If trustId is too old, gets another one from the server.
        if (System.currentTimeMillis() - trustedIdTimestamp > TRUSTED_ID_LIFETIME) {
            trustId = getTrustedTicket(tableauConfig.getTrustedTicketServer(), tableauConfig.getUsername());
        }
        return makeReportUrl(trustId, PASS_REPORT_NAME, projectTitle);
    }

    private String makeReportUrl(String trustId, String reportName, String param) {
        return new StringBuffer()
                .append(serverUrl())
                .append(TRUST_PATH)
                .append(trustId)
                .append(tableauConfig.getReportUrl(reportName))
                .append(param)
                .toString();
    }

    /**
     * Gets a unique trusted ticket from Tableau server.
     *
     * @param wgserver tableau server root url
     * @param user     tableau username
     * @return ticket ("-1" if server could not validate), or null if some other error happened.
     */
    private String getTrustedTicket(String wgserver, String user) {
        OutputStreamWriter out = null;
        BufferedReader in = null;
        try {
            // Encodes parameters
            StringBuffer data = new StringBuffer()
                    .append(URLEncoder.encode("username", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(user, "UTF-8"));

            // Sends request
            URL url = new URL(wgserver + TRUST_PATH);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            out = new OutputStreamWriter(conn.getOutputStream());
            out.write(data.toString());
            out.flush();

            // Reads response which will be the ticket id
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String ticket = new String(IOUtils.toCharArray(in));
            if (ticket.equals(BAD_TABLEAU_TICKET)) {
                logger.warn("Tableau server " + serverUrl() + " returns ticketId " + ticket + " for user " + user);
            }
            return ticket;

        } catch (Exception e) {
            logger.error("Exception while getting trusted ticket from tableau server", e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        return null;
    }
}