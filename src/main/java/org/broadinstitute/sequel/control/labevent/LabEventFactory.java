package org.broadinstitute.sequel.control.labevent;

import org.broadinstitute.sequel.bettalims.jaxb.CherryPickSourceType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.entity.labevent.CherryPickTransfer;
import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventType;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.sequel.entity.run.IlluminaFlowcell;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.StripTube;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptacleType;
import org.broadinstitute.sequel.bettalims.jaxb.StationEventType;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates Lab Event entities from BettaLIMS JAXB beans
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass"})
public class LabEventFactory {

    public static final String SECTION_ALL_96 = "ALL96";

    public static final String PHYS_TYPE_TUBE_RACK = "TubeRack";
    public static final String PHYS_TYPE_EPPENDORF_96 = "Eppendorf96";
    public static final String PHYS_TYPE_STRIP_TUBE_RACK_OF_12 = "StripTubeRackOf12";
    public static final String PHYS_TYPE_STRIP_TUBE = "StripTube";

    private static final Map<String, LabEventType> MAP_MESSAGE_NAME_TO_EVENT_TYPE = new HashMap<String, LabEventType>();

    static {
        LabEventType shearingTransfer = new LabEventType("ShearingTransfer", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(shearingTransfer.getName(), shearingTransfer);

        LabEventType postShearingTransferCleanup = new LabEventType("PostShearingTransferCleanup", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(postShearingTransferCleanup.getName(), postShearingTransferCleanup);

        LabEventType shearingQc = new LabEventType("ShearingQC", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(shearingQc.getName(), shearingQc);

        LabEventType endRepair = new LabEventType("EndRepair", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(endRepair.getName(), endRepair);

        LabEventType endRepairCleanup = new LabEventType("EndRepairCleanup", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(endRepairCleanup.getName(), endRepairCleanup);

        LabEventType aBase = new LabEventType("ABase", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(aBase.getName(), aBase);

        LabEventType aBaseCleanup = new LabEventType("ABaseCleanup", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(aBaseCleanup.getName(), aBaseCleanup);

        LabEventType indexedAdapterLigation = new LabEventType("IndexedAdapterLigation", true, false,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(indexedAdapterLigation.getName(), indexedAdapterLigation);

        LabEventType adapterLigationCleanup = new LabEventType("AdapterLigationCleanup", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(adapterLigationCleanup.getName(), adapterLigationCleanup);

        LabEventType pondEnrichment = new LabEventType("PondEnrichment", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(pondEnrichment.getName(), pondEnrichment);

        LabEventType pondEnrichmentCleanup = new LabEventType("HybSelPondEnrichmentCleanup", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(pondEnrichmentCleanup.getName(), pondEnrichmentCleanup);

        LabEventType pondRegistration = new LabEventType("PondRegistration", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(pondRegistration.getName(), pondRegistration);

        LabEventType preSelectionPool = new LabEventType("PreSelectionPool", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(preSelectionPool.getName(), preSelectionPool);

        LabEventType hybridization = new LabEventType("Hybridization", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(hybridization.getName(), hybridization);

        LabEventType baitSetup = new LabEventType("BaitSetup", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(baitSetup.getName(), baitSetup);

        LabEventType baitAddition = new LabEventType("BaitAddition", true, false,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(baitAddition.getName(), baitAddition);

        LabEventType beadAddition = new LabEventType("BeadAddition", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(beadAddition.getName(), beadAddition);

        LabEventType apWash = new LabEventType("APWash", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(apWash.getName(), apWash);

        LabEventType gsWash1 = new LabEventType("GSWash1", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(gsWash1.getName(), gsWash1);

        LabEventType gsWash2 = new LabEventType("GSWash2", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(gsWash2.getName(), gsWash2);

        LabEventType catchEnrichmentSetup = new LabEventType("CatchEnrichmentSetup", true, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(catchEnrichmentSetup.getName(), catchEnrichmentSetup);

        LabEventType catchEnrichmentCleanup = new LabEventType("CatchEnrichmentCleanup", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(catchEnrichmentCleanup.getName(), catchEnrichmentCleanup);

        LabEventType normalizedCatchRegistration = new LabEventType("NormalizedCatchRegistration", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(normalizedCatchRegistration.getName(), normalizedCatchRegistration);

        LabEventType poolingTransfer = new LabEventType("PoolingTransfer", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(poolingTransfer.getName(), poolingTransfer);

        LabEventType denatureTransfer = new LabEventType("DenatureTransfer", false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(denatureTransfer.getName(), denatureTransfer);

        LabEventType stripTubeBTransfer = new LabEventType("StripTubeBTransfer", true, false,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(stripTubeBTransfer.getName(), stripTubeBTransfer);

        LabEventType flowcellTransfer = new LabEventType("FlowcellTransfer", true, false,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA);
        MAP_MESSAGE_NAME_TO_EVENT_TYPE.put(flowcellTransfer.getName(), flowcellTransfer);
    }
    
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDao;
    private StaticPlateDAO staticPlateDAO;
    private PersonDAO personDAO;

    /**
     * Builds one or more lab event entities from a JAXB message bean that contains one or more event beans
     * @param bettaLIMSMessage JAXB bean
     * @return list of entities
     */
    public List<LabEvent> buildFromBettaLims(BettaLIMSMessage bettaLIMSMessage) {
        List<LabEvent> labEvents = new ArrayList<LabEvent>();
        bettaLIMSMessage.getMode();
        labEvents.add(buildFromBettaLims(bettaLIMSMessage.getPlateCherryPickEvent()));
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            labEvents.add(buildFromBettaLims(plateEventType));
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            labEvents.add(buildFromBettaLims(plateTransferEventType));
        }
        labEvents.add(buildFromBettaLims(bettaLIMSMessage.getReceptaclePlateTransferEvent()));
        return labEvents;
    }

    /**
     * Builds a lab event entity from a JAXB cherry pick event bean
     * @param plateCherryPickEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateCherryPickEvent plateCherryPickEvent) {
        if(plateCherryPickEvent.getPlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE_RACK_OF_12)) {
            return buildCherryPickRackToStripTubeDbFree(null, null, null, null, null);
        }
        return buildCherryPickRackToRackDbFree(null, null, null, null, null);
    }

    public LabEvent buildCherryPickRackToRackDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, VesselContainer> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, VesselContainer> mapBarcodeToTargetRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        for (Map.Entry<String, VesselContainer> stringVesselContainerEntry : mapBarcodeToSourceRack.entrySet()) {
            labEvent.addSourceLabVessel(stringVesselContainerEntry.getValue().getEmbedder());
        }

        for (Map.Entry<String, VesselContainer> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                RackOfTubes targetRack = new RackOfTubes(stringVesselContainerEntry.getKey());
                stringVesselContainerEntry.setValue(targetRack.getVesselContainer());
            }
            labEvent.addTargetLabVessel(stringVesselContainerEntry.getValue().getEmbedder());
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()),
                    cherryPickSourceType.getWell(),
                    mapBarcodeToTargetRack.get(cherryPickSourceType.getDestinationBarcode()),
                    cherryPickSourceType.getDestinationWell()));
        }
        return labEvent;
    }

    public LabEvent buildCherryPickRackToStripTubeDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, VesselContainer> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, VesselContainer> mapBarcodeToTargetRack,
            Map<String, StripTube> mapBarcodeToTargetStripTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        for (Map.Entry<String, VesselContainer> stringVesselContainerEntry : mapBarcodeToSourceRack.entrySet()) {
            labEvent.addSourceLabVessel(stringVesselContainerEntry.getValue().getEmbedder());
        }

        for (Map.Entry<String, VesselContainer> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                // todo jmt do we care about the strip tube holder?
                RackOfTubes targetRack = new RackOfTubes(stringVesselContainerEntry.getKey());
                stringVesselContainerEntry.setValue(targetRack.getVesselContainer());
            }
        }

        Map<String, StripTube> mapPositionToStripTube = new HashMap<String, StripTube>();
        for (ReceptacleType receptacleType : plateCherryPickEvent.getPositionMap().getReceptacle()) {
            if(!receptacleType.getReceptacleType().equals(PHYS_TYPE_STRIP_TUBE)) {
                throw new RuntimeException("Expected physType " + PHYS_TYPE_STRIP_TUBE + ", but received " + receptacleType.getReceptacleType());
            }
            StripTube stripTube = new StripTube(receptacleType.getBarcode());
            labEvent.addTargetLabVessel(stripTube);
            mapPositionToStripTube.put(receptacleType.getPosition(), stripTube);
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String position = cherryPickSourceType.getDestinationWell().substring(1).replaceFirst("^0+(?!$)", "");
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()),
                    cherryPickSourceType.getWell(),
                    mapPositionToStripTube.get(position).getVesselContainer(),
                    Integer.toString(cherryPickSourceType.getDestinationWell().charAt(0) - 'A' + 1)));
        }
        return labEvent;
    }

    /**
    * Builds a lab event entity from a JAXB plate event bean
    * @param plateEventType JAXB event bean
    * @return entity
    */
    public LabEvent buildFromBettaLims(PlateEventType plateEventType) {
        LabEvent labEvent = constructReferenceData(plateEventType);
        return labEvent;
    }

    /**
     * Builds a lab event entity from a JAXB plate transfer event bean
     * @param plateTransferEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent) {
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes = null;
        if(plateTransferEvent.getSourcePositionMap() != null) {
            mapBarcodeToSourceTubes = findTubesByBarcodes(plateTransferEvent.getSourcePositionMap());
        }
        // todo jmt hash the tube positions, fetch any existing tube formation
        StaticPlate targetPlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        
        return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, mapBarcodeToSourceTubes, targetPlate);
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity
     * @param plateTransferEvent JAXB plate transfer event
     * @param mapBarcodeToSourceTubes existing source tubes (in new rack)
     * @param targetPlate existing plate, or null for new plate
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        RackOfTubes rackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap());
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(rackOfTubes);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity
     * @param plateTransferEvent JAXB plate transfer event
     * @param rackOfTubes existing source rack
     * @param targetPlate existing plate, or null for new plate
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes rackOfTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(rackOfTubes);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    /**
     * Build a rack entity
     * @param mapBarcodeToTubes source tubes
     * @param plate JAXB rack
     * @param positionMap JAXB list of tube barcodes
     * @return entity
     */
    private RackOfTubes buildRack(Map<String, TwoDBarcodedTube> mapBarcodeToTubes, PlateType plate, PositionMapType positionMap) {
        RackOfTubes rackOfTubes = new RackOfTubes(plate.getBarcode());
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if(twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(receptacleType.getBarcode(), null);
                mapBarcodeToTubes.put(receptacleType.getBarcode(), twoDBarcodedTube);
            }
            rackOfTubes.getVesselContainer().addContainedVessel(twoDBarcodedTube, receptacleType.getPosition());
        }
        return rackOfTubes;
    }

    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes sourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);

        RackOfTubes targetRackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.addSourceLabVessel(sourceRack);
        labEvent.addTargetLabVessel(targetRackOfTubes);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetRackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes sourceRack,
            RackOfTubes targetRack) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        labEvent.addSourceLabVessel(sourceRack);
        labEvent.addTargetLabVessel(targetRack);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        RackOfTubes rackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.addSourceLabVessel(sourcePlate);
        labEvent.addTargetLabVessel(rackOfTubes);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateEventDbFree(
            PlateEventType plateEvent,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateEvent.getPlate().getBarcode());
        }

        labEvent.addTargetLabVessel(targetPlate);
        return labEvent;
    }
    public LabEvent buildFromBettaLimsPlateToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(sourcePlate);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            StripTube sourceStripTube,
            IlluminaFlowcell targetFlowcell) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetFlowcell == null) {
            // todo jmt what about MiSeq?
            // todo jmt how to populate run configuration?
            targetFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE,
                    plateTransferEvent.getPlate().getBarcode(), null);
        }

        labEvent.addSourceLabVessel(sourceStripTube);
        labEvent.addTargetLabVessel(targetFlowcell);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceStripTube.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetFlowcell.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    public LabEvent buildVesselToSectionDbFree(ReceptaclePlateTransferEvent receptaclePlateTransferEvent,
            TwoDBarcodedTube sourceTube, StaticPlate targetPlate, String targetSection) {
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
        }
        labEvent.addSourceLabVessel(sourceTube);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(sourceTube, targetSection,
                targetPlate.getVesselContainer()));
        return labEvent;
    }

    private Map<String, TwoDBarcodedTube> findTubesByBarcodes(PositionMapType positionMap) {
        List<String> barcodes = new ArrayList<String>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        return this.twoDBarcodedTubeDao.findByBarcodes(barcodes);
    }

    public LabEvent buildFromBettaLims(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent);
        receptaclePlateTransferEvent.getSourceReceptacle();
        return labEvent;
    }
    
    public LabEvent constructReferenceData(StationEventType stationEventType) {
        stationEventType.getComment();
        stationEventType.getDisambiguator();
        stationEventType.getEnd();
        stationEventType.getProgram();
        
        LabEventType labEventType = MAP_MESSAGE_NAME_TO_EVENT_TYPE.get(stationEventType.getEventType());
        if(labEventType == null) {
            throw new RuntimeException("Unexpected event type " + stationEventType.getEventType());
        }
        return new GenericLabEvent(labEventType, stationEventType.getStart().toGregorianCalendar().getTime(),
                stationEventType.getStation(), this.personDAO.findByName(stationEventType.getOperator()));
    }

    public void setTwoDBarcodedTubeDao(TwoDBarcodedTubeDAO twoDBarcodedTubeDao) {
        this.twoDBarcodedTubeDao = twoDBarcodedTubeDao;
    }

    public void setStaticPlateDAO(StaticPlateDAO staticPlateDAO) {
        this.staticPlateDAO = staticPlateDAO;
    }

    public void setPersonDAO(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }
}
