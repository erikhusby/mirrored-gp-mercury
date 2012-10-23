package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

/**
 * Created by IntelliJ IDEA.
 * User: jcarey
 * Date: 10/22/12
 * Time: 1:49 PM
 * <p/>
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 * <p/>
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
public abstract class LabVesselDao {
    public abstract LabVessel findByBarcode(String barcode);
}
