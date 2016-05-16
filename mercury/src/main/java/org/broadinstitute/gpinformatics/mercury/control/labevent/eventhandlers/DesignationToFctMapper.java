package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * For testing of Squid messages with Mercury, builds a map from Squid designation to FCT ticket.
 */
public class DesignationToFctMapper {
    private static Map<String, String> mapDesignationToFct;

    private static final String DIR = "C:\\Users\\thompson\\Downloads\\";
    private static String[] jiraExportFileNames = {
            "Fct201510.csv",
            "Fct201511.csv",
            "Fct201512.csv",
            "Fct201601.csv",
            "Fct201602.csv",
            "Fct201603.csv",
            "Fct201604.csv",
            "Fct201605.csv",
    };

    public static String getFctForDesignation(String designation) {
        try {
            if (mapDesignationToFct == null) {
                mapDesignationToFct = new HashMap<>();
                for (String jiraExportFileName : jiraExportFileNames) {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(DIR + jiraExportFileName));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.startsWith("FCT-")) {
                            String[] fields = line.split(";");
                            if (fields.length >= 2) {
                                String fct = fields[0];
                                String designationWithQuotes = fields[1];
                                if (!designationWithQuotes.isEmpty()) {
                                    mapDesignationToFct.put(
                                            designationWithQuotes.substring(1, designationWithQuotes.length() - 1), fct);
                                }
                            }
                        }
                    }
                }
            }
            return mapDesignationToFct.get(designation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
