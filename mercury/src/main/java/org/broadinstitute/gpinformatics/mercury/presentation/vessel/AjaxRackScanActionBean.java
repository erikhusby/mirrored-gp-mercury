package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationError;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@UrlBinding("/vessel/AjaxRackScan.action")
public class AjaxRackScanActionBean extends RackScanActionBean {

    public static final String AJAX_SELECT_LAB_EVENT = "ajaxLabSelect";
    public static final String AJAX_SCAN_EVENT = "ajaxScan";

    /**
     * Returns the ajax modal dialog content when lab selection is changed
     */
    @HandlesEvent(AJAX_SELECT_LAB_EVENT)
    public Resolution selectLab() {
        return new ForwardResolution("/vessel/ajax_div_rack_scanner.jsp");
    }

    /**
     * Returns either a JSON scan data object or errors to a client side ajax function callback
     * @throws ScannerException
     */
    @HandlesEvent(AJAX_SCAN_EVENT)
    public Resolution ajaxScan() throws ScannerException {
        final JSONObject scannerData = new JSONObject();
        final StringBuilder errors = new StringBuilder();
        try {
            // Run the rack scanner and include the rack barcode in position map
            runRackScan(true);

            // Should never happen
            if( rackScan == null || rackScan.isEmpty() ){
                errors.append("No results from rack scan");
            }

            // Check for scan errors
            if( getValidationErrors().isEmpty()) {
                // Scan data can be persisted with a SearchInstance, keep track of who ran it and when
                scannerData.put("scanDate", ColumnValueType.DATE_TIME.format(new Date(),""));
                scannerData.put("scanUser", getUserBean().getLoginUserName());
                scannerData.put("scannerName", getRackScanner().getScannerName());
                JSONArray scan = new JSONArray();
                scannerData.put("scans", scan);
                for( Map.Entry<String,String> positionAndBarcode : rackScan.entrySet() ) {
                    if( positionAndBarcode.getKey().equals("rack")) {
                        scannerData.put("rackBarcode", positionAndBarcode.getValue());
                        continue;
                    }
                    if( StringUtils.isNotEmpty( positionAndBarcode.getValue() ) ) {
                        scan.put(new JSONObject()
                                .put("position", positionAndBarcode.getKey())
                                .put("barcode", positionAndBarcode.getValue())
                        );
                    }
                }
                if( scan.length() == 0 ){
                    errors.append("No results from rack scan");
                }
            } else {
                for( Map.Entry<String, List<ValidationError>> errorEntry : getValidationErrors().entrySet() ) {
                    for( ValidationError error : errorEntry.getValue() ) {
                        errors.append( error.getMessage(Locale.getDefault()) );
                    }
                }
            }
        } catch (Exception ex){
            log.error(ex);
            errors.append("Rack scan error occurred: " + ex.getMessage());
        }

        return new StreamingResolution("text/plain") {
            @Override
            public void stream(HttpServletResponse response) throws Exception {
                ServletOutputStream out = response.getOutputStream();
                if(errors.length() > 0 ) {
                    // TODO JMS This should be a JSON object to be consistent
                    out.write("Failure: ".getBytes());
                    out.write(errors.toString().getBytes());
                } else {
                    out.write(scannerData.toString().getBytes());
                }
                out.close();
            }
        };
    }

    @Override
    public String getRackScanPageUrl() {
        return "/vessel/AjaxRackScan.action";
    }

    @Override
    public String getPageTitle() {
        return "Perform Rack Scan";
    }


}
