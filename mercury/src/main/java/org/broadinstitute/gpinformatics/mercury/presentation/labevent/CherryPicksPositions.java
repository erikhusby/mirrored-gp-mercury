package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import java.util.ArrayList;
import java.util.List;

public class CherryPicksPositions {
    public List<String> sourceIDs = new ArrayList<String>();
    public List<String> targetIDs = new ArrayList<String>();
    public List<String> sourceBarcodes = new ArrayList<String>();
    public List<String> targetBarcodes = new ArrayList<String>();
    public List<String> targetFCT = new ArrayList<String>(); //Used for Strip Tube transfers
    public List<String> targetPositions = new ArrayList<String>(); //Used for Strip Tube transfers
}
