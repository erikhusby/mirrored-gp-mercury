package org.broadinstitute.gpinformatics.mercury.presentation.run;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.math.BigDecimal;
import java.util.Comparator;

/** This interface defines the subset of Dto data needed to create an FCT. */
public interface FctDto {
    public String getBarcode();
    public String getLcset();
    public BigDecimal getLoadingConc();
    public Integer getNumberLanes();
    public boolean isAllocated();
    public void setAllocated(boolean wasAllocated);
    public int getAllocationOrder();
    public FctDto split(int numberLanes);


    public static final Comparator<Pair<FctDto, LabVessel>> BY_ALLOCATION_ORDER =
            new Comparator<Pair<FctDto, LabVessel>>() {
                @Override
                public int compare(Pair<FctDto, LabVessel> o1, Pair<FctDto, LabVessel> o2) {
                    // Puts highest allocationOrder first. If it's a tie, puts highest numberLanes first.
                    if (o2.getLeft().getAllocationOrder() != o1.getLeft().getAllocationOrder()) {
                        return o2.getLeft().getAllocationOrder() - o1.getLeft().getAllocationOrder();
                    } else {
                        return (o2.getLeft().getNumberLanes() != null & o1.getLeft().getNumberLanes() != null) ?
                                o2.getLeft().getNumberLanes().compareTo(o1.getLeft().getNumberLanes()) : 0;
                    }
                }
            };
}
