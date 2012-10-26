package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// todo jmt rename to TubeFormation, create separate (non-LabVessel) class for rack
/**
 * A rack of tubes
 */
@Entity
@Audited
@Table(schema = "mercury")
public class RackOfTubes extends LabVessel implements VesselContainerEmbedder<TwoDBarcodedTube> {

    public enum RackType {
        Matrix96("Matrix96", VesselGeometry.G12x8);

        private final String displayName;
        private final VesselGeometry vesselGeometry;

        RackType(String displayName, VesselGeometry vesselGeometry) {
            //To change body of created methods use File | Settings | File Templates.
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        public String getDisplayName() {
            return displayName;
        }

        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }
    }

    // todo jmt can't make this non-null, because all LabVessels subtypes are in the same table
    // todo jmt unique constraint?
//    @Column(nullable = false)
    private String digest;

    @Enumerated(EnumType.STRING)
    private RackType rackType;

    @Embedded
    private VesselContainer<TwoDBarcodedTube> vesselContainer = new VesselContainer<TwoDBarcodedTube>(this);

    public RackOfTubes(String label, RackType rackType) {
        super(label);
        this.rackType = rackType;
    }

    protected RackOfTubes() {
    }

    public RackType getRackType() {
        return rackType;
    }

    public void makeDigest() {
        List<Map.Entry<VesselPosition, String>> positionBarcodeList = new ArrayList<Map.Entry<VesselPosition, String>>();
        for (Map.Entry<VesselPosition, TwoDBarcodedTube> barcodedTubeEntry : vesselContainer.getMapPositionToVessel().entrySet()) {
            positionBarcodeList.add(new AbstractMap.SimpleEntry<VesselPosition, String>(
                    barcodedTubeEntry.getKey(), barcodedTubeEntry.getValue().getLabel()));
        }
        this.digest = makeDigest(positionBarcodeList);
    }

    public static String makeDigest(List<Map.Entry<VesselPosition, String>> positionBarcodeList) {
        Collections.sort(positionBarcodeList, new Comparator<Map.Entry<VesselPosition, String>>() {
            @Override
            public int compare(Map.Entry<VesselPosition, String> o1, Map.Entry<VesselPosition, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<VesselPosition, String> positionBarcodeEntry : positionBarcodeList) {
            stringBuilder.append(positionBarcodeEntry.getKey());
            stringBuilder.append(positionBarcodeEntry.getValue());
        }
        return makeDigest(stringBuilder.toString());
    }

    public static String makeDigest(String positionBarcodeTupleString) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(positionBarcodeTupleString.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return this.getVesselContainer().getSampleInstances();
    }

    @Override
    public Float getVolume() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getConcentration() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        return this.vesselContainer.getTransfersFrom();
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        return this.vesselContainer.getTransfersTo();
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.RACK_OF_TUBES;
    }

    public VesselContainer<TwoDBarcodedTube> getVesselContainer() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<TwoDBarcodedTube> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }

    public void addTube(TwoDBarcodedTube tube,VesselPosition wellLocation) {
        if (vesselContainer.getVesselAtPosition(wellLocation) != null) {
            throw new RuntimeException(vesselContainer.getVesselAtPosition(wellLocation) + " is already in position " + wellLocation + " on rack " + getLabel());
        }
        vesselContainer.addContainedVessel(tube,wellLocation);
    }

/*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(Hibernate.getClass(o).equals(RackOfTubes.class))) return false;

        RackOfTubes that = (RackOfTubes) o;

        if (!this.label.equals(that.getLabel())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return this.label.hashCode();
    }
*/

    public String getDigest() {
        return digest;
    }


}
