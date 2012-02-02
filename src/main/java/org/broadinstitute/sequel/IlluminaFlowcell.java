package org.broadinstitute.sequel;


import java.util.ArrayList;
import java.util.Collection;

public class IlluminaFlowcell extends AbstractRunCartridge implements UserRemarkable {

    private Collection<RunChamber> runChambers = new ArrayList<RunChamber>();

    private IlluminaRunConfiguration runConfiguration;

    private FLOWCELL_TYPE flowcellType;

    protected IlluminaFlowcell(String label) {
        super(label);
        this.flowcellBarcode = label;
    }

    public enum FLOWCELL_TYPE {
        EIGHT_LANE,MISEQ
    }

    private final String flowcellBarcode;


    public IlluminaFlowcell(FLOWCELL_TYPE flowcellType,String flowcellBarcode,IlluminaRunConfiguration runConfig) {
        super(flowcellBarcode);
        this.flowcellBarcode = flowcellBarcode;
        this.runConfiguration = runConfig;
        this.flowcellType = flowcellType;
    }
        
    public void addChamber(Goop library,int laneNumber) {
        if (flowcellType == FLOWCELL_TYPE.EIGHT_LANE) {
            if (laneNumber < 1 || laneNumber > 8) {
                throw new RuntimeException("Lane numbers are 1-8");
            }
        }
        else if (flowcellType == FLOWCELL_TYPE.MISEQ) {
            if (laneNumber != 1) {
                throw new RuntimeException("Miseq flowcells only have a single lane");
            }
        }
        IlluminaRunChamber runChamber = new IlluminaRunChamber(this,laneNumber,library);
        runChambers.add(runChamber);
    }
    
    /**
     * In the illumina world, one sets the run configuration
     * when the flowcell is made.  But other technologies
     * might have their run configuration set later
     * in the process.
     * @return
     */
    public IlluminaRunConfiguration getRunConfiguration() {
        return this.runConfiguration;
    }

    @Override
    public Iterable<RunChamber> getChambers() {
        return runChambers;
    }

    @Override
    public String getCartridgeName() {
        return this.flowcellBarcode;
    }

    @Override
    public String getCartridgeBarcode() {
        return this.flowcellBarcode;
    }


}
