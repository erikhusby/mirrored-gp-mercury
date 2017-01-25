package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    /**
     * The list returned from this must be deterministic.
     * @param rowObject object from result row
     * @param expressionClass class against which expression will be evaluated
     * @param context search parameters
     * @param <T> expression class
     * @return list of T classes, must be in same order for repeated calls
     */
    public static <T> List<T> rowObjectToExpressionObject(@Nonnull Object rowObject, Class<T> expressionClass,
            SearchContext context) {
        if (OrmUtil.proxySafeIsInstance(rowObject, LabVessel.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            LabVessel labVessel = (LabVessel) rowObject;
            return (List<T>) new ArrayList<>(labVessel.getSampleInstancesV2());
        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabVessel.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            LabVessel labVessel = (LabVessel) rowObject;
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
            return (List<T>) results;
        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabEvent.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            LabEvent labEvent = (LabEvent) rowObject;
            LabVessel labVessel = labEvent.getInPlaceLabVessel();
            if (labVessel == null) {
                Set<LabVessel> labVessels;
                LabEventType.PlasticToValidate plasticToValidate = labEvent.getLabEventType().getPlasticToValidate();
                switch (plasticToValidate) {
                    case BOTH:
                    case SOURCE:
                        labVessels = labEvent.getSourceLabVessels();
                        break;
                    case TARGET:
                        labVessels = labEvent.getTargetLabVessels();
                        break;
                    default:
                        throw new RuntimeException("Unexpected enum " + plasticToValidate);
                }
                // todo jmt sort lab vessels?
                Set<SampleInstanceV2> sampleInstances = new TreeSet<>();
                for (LabVessel vessel : labVessels) {
                    sampleInstances.addAll(vessel.getSampleInstancesV2());
                }
                return (List<T>) new ArrayList<>(sampleInstances);
            } else {
                return (List<T>) new ArrayList<>(labVessel.getSampleInstancesV2());
            }
        } else {
            throw new RuntimeException("Unexpected combination " + rowObject.getClass() + " to " + expressionClass);
        }
    }
}

