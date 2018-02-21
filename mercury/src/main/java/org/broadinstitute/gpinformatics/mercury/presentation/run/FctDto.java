package org.broadinstitute.gpinformatics.mercury.presentation.run;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** This interface defines the subset of Dto data needed to create an FCT. */
public interface FctDto {
    public String getBarcode();
    public String getLcset();
    public BigDecimal getLoadingConc();
    public Integer getNumberLanes();
    public boolean isAllocated();
    public void setAllocated(boolean wasAllocated);
    public int getAllocationOrder();
    public FctDto split(int numberAlocatedLanes);
    public String getProduct();
    public List<String> getProductNames();
    public <DTO_TYPE extends FctDto> boolean isCompatible(Collection<DTO_TYPE> groupDtos);
}
