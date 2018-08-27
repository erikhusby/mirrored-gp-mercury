package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graphs for the Single Cell 10X process.
 */
public class SingleCell10XEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;
    private final StaticPlate sourceplate;
    private int numSamples;
    private StaticPlate aTailPlate;
    private StaticPlate aTailCleanupPlate;
    private StaticPlate ligationCleanupPlate;
    private StaticPlate doubleSidedSpriPlate;

    public SingleCell10XEntityBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, StaticPlate sourceplate, int numSamples, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.sourceplate = sourceplate;
        this.numSamples = numSamples;
        this.testPrefix = testPrefix;
    }

    public SingleCell10XEntityBuilder invoke() {

        final String indexPlateBarcode = "SC10XIndexPlate" + testPrefix;
        List<StaticPlate> indexPlatesList = LabEventTest.buildIndexPlate(null, null,
                new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                }},
                new ArrayList<String>() {{
                    add(indexPlateBarcode);
                }}
        );

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();

        SingleCell10XJaxbBuilder jaxbBuilder = new SingleCell10XJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                sourceplate.getLabel(), indexPlateBarcode).invoke();

        LabEventTest.validateWorkflow("SingleCellEndRepairABase", sourceplate);
        mapBarcodeToVessel.put(sourceplate.getLabel(), sourceplate);
        LabEvent endRepaiAbaseEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getEndRepairAbaseJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(endRepaiAbaseEntity);
        aTailPlate = (StaticPlate) endRepaiAbaseEntity.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SingleCellATailCleanup", aTailPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(aTailPlate.getLabel(), aTailPlate);
        LabEvent aTailCleanupEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getaTailCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(aTailCleanupEntity);
        aTailCleanupPlate = (StaticPlate) aTailCleanupEntity.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SingleCellAdapterLigation", aTailCleanupPlate);
        LabEvent adapterLigationEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                jaxbBuilder.getAdapterLigationJaxb(), aTailCleanupPlate);
        labEventHandler.processEvent(adapterLigationEntity);

        LabEventTest.validateWorkflow("SingleCellLigationCleanup", aTailCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(aTailCleanupPlate.getLabel(), aTailCleanupPlate);
        LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getLigationCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(ligationCleanupEntity);
        ligationCleanupPlate = (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SingleCellIndexAdapterPCR", ligationCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(indexPlatesList.get(0).getLabel(), indexPlatesList.get(0));
        mapBarcodeToVessel.put(ligationCleanupPlate.getLabel(), ligationCleanupPlate);
        LabEvent indexAdapterPCREntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getIndexAdapterPCRJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(indexAdapterPCREntity);
        ligationCleanupPlate.clearCaches();

        LabEventTest.validateWorkflow("SingleCellDoubleSidedCleanup", ligationCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(ligationCleanupPlate.getLabel(), ligationCleanupPlate);
        LabEvent doubleSidedSpriEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getDoubleSidedSpriJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(doubleSidedSpriEntity);
        doubleSidedSpriPlate = (StaticPlate) doubleSidedSpriEntity.getTargetLabVessels().iterator().next();

        return this;
    }

    public StaticPlate getDoubleSidedSpriPlate() {
        return doubleSidedSpriPlate;
    }
}
