package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Expressions for configurable columns, grouped by ExpressionClass.
 */
public enum DisplayExpression {

    // SampleInstance
    ROOT_SAMPLE_ID(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getRootOrEarliestMercurySampleName();
        }
    }),
    ROOT_TUBE_BARCODE(SampleInstanceV2.class, new SearchTerm.Evaluator<Set<String>>() {
        @Override
        public Set<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            Set<String> results = new HashSet<>();
            if(sampleInstanceV2.getRootOrEarliestMercurySample() != null){
                for (LabVessel rootSampleVessel : sampleInstanceV2.getRootOrEarliestMercurySample()
                        .getLabVessel()) {
                    results.add(rootSampleVessel.getLabel());
                }
            }
            return results;
        }
    }),
    NEAREST_SAMPLE_ID(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getNearestMercurySampleName();
        }
    }),
    PRODUCT_NAME(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleProductOrderSample first
            for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                if (productOrderSample.getProductOrder().getProduct() != null) {
                    results.add(productOrderSample.getProductOrder().getProduct().getDisplayName());
                }
            }
            return results;
        }
    }),
    MOLECULAR_INDEX(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
            return molecularIndexingScheme == null ? null : molecularIndexingScheme.getName();
        }
    }),
    METADATA(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            SearchTerm searchTerm = context.getSearchTerm();
            String metaName = searchTerm.getName();
            Metadata.Key key = Metadata.Key.fromDisplayName(metaName);
            if (key == Metadata.Key.MATERIAL_TYPE) {
                MaterialType materialType = sampleInstanceV2.getMaterialType();
                return materialType == null ? null : materialType.getDisplayName();
            }

            MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
            if (mercurySample != null) {
                Set<Metadata> metadata = mercurySample.getMetadata();
                if( metadata != null && !metadata.isEmpty() ) {
                    for( Metadata meta : metadata){
                        if( meta.getKey() == key ) {
                            return meta.getValue();
                        }
                    }
                }
            }
            return null;
        }
    }),

    // SampleData
    STOCK_SAMPLE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getStockSample();
        }
    }),
    COLLABORATOR_SAMPLE_ID(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getCollaboratorsSampleName();
        }
    }),
    COLLABORATOR_PARTICIPANT_ID(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getCollaboratorParticipantId();
        }
    }),
    SAMPLE_TYPE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getSampleType();
        }
    }),
    COLLECTION(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getCollection();
        }
    }),
    ORIGINAL_MATERIAL_TYPE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getOriginalMaterialType();
        }
    }),
    ;

    private final Class<?> expressionClass;
    private final SearchTerm.Evaluator<?> evaluator;

    DisplayExpression(Class<?> expressionClass, SearchTerm.Evaluator<?> evaluator) {
        this.expressionClass = expressionClass;
        this.evaluator = evaluator;
    }

    public Class<?> getExpressionClass() {
        return expressionClass;
    }

    public SearchTerm.Evaluator<?> getEvaluator() {
        return evaluator;
    }

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
            // LabVessel to SampleInstance
            LabVessel labVessel = (LabVessel) rowObject;
            return (List<T>) new ArrayList<>(labVessel.getSampleInstancesV2());

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabVessel.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            // LabVessel to SampleData
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
                BspSampleSearchAddRowsListener bspColumns = (BspSampleSearchAddRowsListener) context.getRowsListener(
                        BspSampleSearchAddRowsListener.class.getSimpleName());
                for( MercurySample mercurySample : mercurySamples) {
                    results.add(bspColumns.getSampleData(mercurySample.getSampleKey()));
                }
            }
            return (List<T>) results;

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabEvent.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // LabEvent to SampleInstance
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
                Set<SampleInstanceV2> sampleInstances = new TreeSet<>();
                for (LabVessel vessel : labVessels) {
                    sampleInstances.addAll(vessel.getSampleInstancesV2());
                }
                return (List<T>) new ArrayList<>(sampleInstances);
            } else {
                return (List<T>) new ArrayList<>(labVessel.getSampleInstancesV2());
            }

        } else if (OrmUtil.proxySafeIsInstance(rowObject, MercurySample.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // MercurySample to SampleInstance
            MercurySample mercurySample = (MercurySample) rowObject;
            Set<SampleInstanceV2> sampleInstances = new TreeSet<>();
            for (LabVessel labVessel : mercurySample.getLabVessel()) {
                sampleInstances.addAll(labVessel.getSampleInstancesV2());
            }
            return (List<T>) new ArrayList<>(sampleInstances);

        } else {
            throw new RuntimeException("Unexpected combination " + rowObject.getClass() + " to " + expressionClass);
        }
    }
}
