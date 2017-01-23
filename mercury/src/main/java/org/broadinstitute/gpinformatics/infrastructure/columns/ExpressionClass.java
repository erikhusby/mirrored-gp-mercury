package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Enumerates classes to which DisplayExpressions can be applied.
 */
public enum ExpressionClass {
    SAMPLE_INSTANCE,
    // PDO 2, PRODUCT 2, RESEARCH_PROJECT, LCSET 2, XTR, FCT, NEAREST_SAMPLE_ID 2, ROOT_SAMPLE_ID 2, MOLECULAR_INDEX,
    // SAMPLE_TUBE_BARCODE
    LAB_VESSEL,
    // BARCODE 2 (SAMPLE_TUBE_BARCODE?), PLATE_NAME, VESSEL_TYPE, VESSEL_VOLUME, VESSEL_CONCENTRATION, SAMPLE_HISTORY,
    // BUCKET_COUNT, IN_PLACE_EVENT_TYPE, DNA_EXTRACTED_TUBE_BARCODE, RNA_EXTRACTED_TUBE_BARCODE,
    // IMPORTED_SAMPLE_TUBE_BARCODE, IMPORTED_SAMPLE_ID, IMPORTED_SAMPLE_POSITION, POND SAMPLE POSITION,
    // POND_TUBE_BARCODE, SHEARING_SAMPLE_POSITION, SHEARING_SAMPLE_BARCODE, CATCH_SAMPLE_POSITION, CATCH_TUBE_BARCODE,
    // FLOWCELL_BARCODE, SEQUENCING_RUN_NAME, EMERGE_VOLUME_TRANSFER [SM-ID, RACK BARCODE, RACK POSITION],
    // RACK_BARCODE, RACK_POSITION, DOWNSTREAM_LCSET, DOWNSTREAM_XTR, DOWNSTREAM_FCT, TOTAL_NG_INITIAL_PICO,
    // METADATA_VALUE, ABANDON_REASON, ABANDON_DATE,
    // SCAN_DATE, SCANNER_NAME, SCAN_USER, SCAN_RACK_BARCODE, SCAN_POSITION,
    // DNA_PLATE_WELL, DNA_ARRAY_PLATE_BARCODE, AMP_PLATE_WELL, AMP_PLATE_BARCODE, CHIP_WELL,
    // CHIP_BARCODE, DNA_PLATE_DRILLDOWN, AMP_PLATE_DRILLDOWN, CHIP_DRILLDOWN, HYB_CHAMBER, TECAN_POSITION,
    // INITIAL_PICO, PROCEED_IF_OOS
    LAB_EVENT,
    // LAB_EVENT_ID, EVENT_DATE, EVENT_LOCATION, EVENT_OPERATOR, EVENT_TYPE, PROGRAM_NAME, SIMULATION_MODE,
    // nested reagents?, SOURCE_LAB_VESSEL_TYPE, SOURCE_BARCODE, DESTINATION_LAB_VESSEL_TYPE, DESTINATION_BARCODE
    REAGENT,
    // REAGENT_TYPE, REAGENT_LOT, REAGENT_EXPIRATION,
    METADATA,
    SAMPLE_DATA; // or BSP?  Mercury sampledata is searchable, BSP is not, so the search terms have to be system specific,
    // but perhaps the display expressions could be off SAMPLE_DATA

    public static <Y> List<Y> xToY(Object x, Class<Y> yClass, SearchContext context) {
        if (OrmUtil.proxySafeIsInstance(x, LabVessel.class) && yClass.isAssignableFrom(SampleInstanceV2.class)) {
            LabVessel labVessel = (LabVessel) x;
            return (List<Y>) new ArrayList<>(labVessel.getSampleInstancesV2());
        } else if (OrmUtil.proxySafeIsInstance(x, LabVessel.class) && yClass.isAssignableFrom(SampleData.class)) {
            LabVessel labVessel = (LabVessel) x;
            List<MercurySample> mercurySamples = new ArrayList<>();
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
                if (mercurySample != null) {
                    mercurySamples.add(mercurySample);
                }
            }

            List<SampleData> results = new ArrayList<>();
            if (!mercurySamples.isEmpty()) {
                BspSampleSearchAddRowsListener bspColumns =
                        (BspSampleSearchAddRowsListener) context.getRowsListener(BspSampleSearchAddRowsListener.class.getSimpleName());
                for( MercurySample mercurySample : mercurySamples) {
                    results.add(bspColumns.getSampleData(mercurySample.getSampleKey()));
                }
            }
            return (List<Y>) results;
        } else {
            throw new RuntimeException("Unexpected combination " + x.getClass() + " to " + yClass);
        }
    }
}

