/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.kits;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SampleKitRequestDto {
    private String requestedBy;
    private List<String> projectManagers;
    private String plasticware;
    private int numberOfRacks;
    private int numberOfTubesPerRack;
    private String destination;
    private String deliveryMethod;
    private String bspKitRequest;
    private String linkedProductOrder;

    /**
     * Empty constructor for action beans to use to bind form elements to.
     */
    public SampleKitRequestDto() {}

    /**
     * @param requestedBy          User requesting the sample kit.
     * @param projectManagers      list of projectManagers
     * @param plasticware          Can choose from a drop down list of plasticware (currently restricted to
     *                             0.75 mL tubes that have linear barcodes etched/printed on the side by the manufacturer
     * @param numberOfRacks        How many empty racks to request.
     * @param numberOfTubesPerRack How many tubes per rack.
     * @param destination          The destination for the kit.
     * @param deliveryMethod       How will we deliver the kit? (Fedex, Broad Truck, Local Pickup)
     * @param bspKitRequest        The BSP kit request this should be linked to.
     * @param linkedProductOrder   The Mercury ProductOrder this request should be linked to.
     */
    public SampleKitRequestDto(@Nonnull String requestedBy, List<String> projectManagers,
                               @Nonnull String plasticware,
                               @Nonnull int numberOfRacks,
                               @Nonnull int numberOfTubesPerRack, @Nonnull String destination,
                               @Nullable String deliveryMethod, @Nullable String bspKitRequest,
                               @Nonnull String linkedProductOrder) {
        this.requestedBy = requestedBy;
        this.projectManagers = projectManagers;
        this.plasticware = plasticware;
        this.numberOfRacks = numberOfRacks;
        this.numberOfTubesPerRack = numberOfTubesPerRack;
        this.destination = destination;
        this.deliveryMethod = deliveryMethod;
        this.bspKitRequest = bspKitRequest;
        this.linkedProductOrder = linkedProductOrder;
    }


    public List<String> getProjectManagers() {
        return projectManagers;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public String getPlasticware() {
        return plasticware;
    }

    public void setPlasticware(String plasticware) {
        this.plasticware = plasticware;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public int getNumberOfRacks() {
        return numberOfRacks;
    }

    public void setNumberOfRacks(int numberOfRacks) {
        this.numberOfRacks = numberOfRacks;
    }

    public int getNumberOfTubesPerRack() {
        return numberOfTubesPerRack;
    }

    public void setNumberOfTubesPerRack(int numberOfTubesPerRack) {
        this.numberOfTubesPerRack = numberOfTubesPerRack;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getBspKitRequest() {
        return bspKitRequest;
    }

    public String getLinkedProductOrder() {
        return linkedProductOrder;
    }
}
