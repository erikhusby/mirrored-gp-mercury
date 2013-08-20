package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;


public class BSPPlatingRequestOptions {

    /**
     * These can't really stay enums since the underlying options are subject to
     * change on the BSP side. I think Jason has an unreleased version of BSP
     * client code that may take care of more strongly typing these items and
     * allow for runtime enumeration.
     *
     * @author mcovarr
     */
    public enum HighConcentrationOption {
        VOLUME_FIRST,
        CONCENTRATION_FIRST,
        OMIT
    }

    public enum PlatformAndProcess {
        ILLUMINA_HYBRID_SELECTION_WGS_FRAGMENT_180BP,
        ILLUMINA_6_14Kb_JUMP,
        ILLUMINA_cDNA_OTHER,
        ILLUMINA_FOSILL_LC,
        _454_FRAGMENT_LC,
        _454_16S_PLAN_A
    }


    public enum PlateType {
        Matrix96SlotRackSC05
        // Matrix96SlotRackSC14;        
    }

    public enum TubeType {
        MatrixTubeSC05

        // Andrew thought MatrixTubeSC14 was the proper tube type to use with the
        // ILLUMINA_HYBRID_SELECTION_WGS_FRAGMENT_180BP platform/process, but
        // the BSP API gives me a warning (not an error) if I choose this. I
        // need to follow up on this with Andrew and Alex to make sure Andrew's
        // expectation was correct, and that a warning is the appropriate
        // response from BSP if an unsupported tube type is requested.

        // MatrixTubeSC14; 
    }

    public enum CancerProject {
        YES, NO
    }

    public enum AllowLessThanOne {
        YES, NO
    }

    public BSPPlatingRequestOptions(HighConcentrationOption highConcentrationOption, PlatformAndProcess platformAndProcess, PlateType plateType,
                                    TubeType tubeType, AllowLessThanOne allowLessThanOne, CancerProject cancerProject) {

        setHighConcentrationOption(highConcentrationOption);
        setPlatformAndProcess(platformAndProcess);
        setPlateType(plateType);
        setTubeType(tubeType);
        setAllowLessThanOne(allowLessThanOne);
        setCancerProject(cancerProject);

    }

    private HighConcentrationOption highConcentrationOption;

    private PlatformAndProcess platformAndProcess;

    private PlateType plateType;

    private TubeType tubeType;

    public AllowLessThanOne getAllowLessThanOne() {
        return allowLessThanOne;
    }


    public void setAllowLessThanOne(AllowLessThanOne allowLessThanOne) {
        if (allowLessThanOne == null)
            throw new RuntimeException("AllowLessThanOne can not be null");
        this.allowLessThanOne = allowLessThanOne;
    }


    public CancerProject getCancerProject() {
        return cancerProject;
    }


    public void setCancerProject(CancerProject cancerProject) {
        if (cancerProject == null)
            throw new RuntimeException("Cancer project can not be null");
        this.cancerProject = cancerProject;
    }

    private AllowLessThanOne allowLessThanOne;

    private CancerProject cancerProject;

    private String notificationList; //list of emails comma separated

    private String platingStyle = "VOLUME_CONC";

    private String labelOption = "FULL_LABELING";

    private Boolean fixPlateGaps = Boolean.FALSE;

    public HighConcentrationOption getHighConcentrationOption() {
        return highConcentrationOption;
    }


    public void setHighConcentrationOption(HighConcentrationOption highConcentrationOption) {
        if (highConcentrationOption == null)
            throw new RuntimeException("High concentration option must not be null");
        this.highConcentrationOption = highConcentrationOption;
    }


    public PlatformAndProcess getPlatformAndProcess() {
        return platformAndProcess;
    }


    public void setPlatformAndProcess(PlatformAndProcess platformAndProcess) {
        if (platformAndProcess == null)
            throw new RuntimeException("Platform and Process can not be null");
        this.platformAndProcess = platformAndProcess;
    }


    public PlateType getPlateType() {
        return plateType;
    }


    public void setPlateType(PlateType plateType) {
        if (plateType == null)
            throw new RuntimeException("Plate type can not be null");
        this.plateType = plateType;
    }


    public TubeType getTubeType() {
        return tubeType;
    }


    public void setTubeType(TubeType tubeType) {
        if (tubeType == null)
            throw new RuntimeException("Tube type can not be null");
        this.tubeType = tubeType;
    }


    public String getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(String notificationList) {
        this.notificationList = notificationList;
    }

    public String getPlatingStyle() {
        return platingStyle;
    }

    public String getLabelOption() {
        return labelOption;
    }

    public Boolean getFixPlateGaps() {
        return fixPlateGaps;
    }
}
