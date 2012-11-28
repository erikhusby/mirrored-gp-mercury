package org.broadinstitute.gpinformatics.infrastructure.reports;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Servlet implementation class for Servlet: TableauServlet
 */
public class TableauServlet extends javax.servlet.http.HttpServlet {
    private static final long serialVersionUID = 1L;

    public TableauServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String user = "epolk";
        final String wgserver = "https://tableau.broadinstitute.org";
        final String dst = "views/AssemblyMetrics/StandardMetrics";
        final String params = ":embed=yes&:toolbar=yes";

        String ticket = getTrustedTicket(wgserver, user, request.getRemoteAddr());
        if (!ticket.equals("-1")) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", wgserver + "/trusted/" + ticket + "/" + dst + "?" + params);
        } else {
            throw new ServletException("Ticket request failed: " + ticket);
        }
    }

    /**
     * Gets a unique trusted ticket from Tableau server.
     *
     * @param wgserver tableau server root url
     * @param user     tableau username
     * @param clientIp client IP address, not needed unless wgserver.extended_trusted_ip_checking is enabled on the server.
     * @return
     * @throws ServletException
     */
    private String getTrustedTicket(String wgserver, String user, String clientIp) throws ServletException {
        OutputStreamWriter out = null;
        BufferedReader in = null;
        try {
            // Encodes parameters
            StringBuffer data = new StringBuffer()
                    .append(URLEncoder.encode("username", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(user, "UTF-8"));
            data.append("&")
                    .append(URLEncoder.encode("client_ip", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(clientIp, "UTF-8"));

            // Sends request
            URL url = new URL(wgserver + "/trusted");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            out = new OutputStreamWriter(conn.getOutputStream());
            out.write(data.toString());
            out.flush();

            // Reads response
            StringBuffer rsp = new StringBuffer();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                rsp.append(line);
            }

            return rsp.toString();

        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
            try {
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }
}
