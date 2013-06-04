package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A set of tubes in a particular combination of positions, typically held in a RackOfTubes.  This entity is different
 * from RackOfTubes, because racks can be reused to hold different sets of tubes.
 */
@Entity
@Audited
public class TubeFormation extends LabVessel implements VesselContainerEmbedder<TwoDBarcodedTube> {

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury", name = "LAB_VESSEL_RACKS_OF_TUBES",
                      joinColumns = @JoinColumn(name = "LAB_VESSEL"),
                      inverseJoinColumns = @JoinColumn(name = "RACKS_OF_TUBES"))
    private Set<RackOfTubes> racksOfTubes = new HashSet<RackOfTubes>();

    @Enumerated(EnumType.STRING)
    private RackOfTubes.RackType rackType;

    @Embedded
    private VesselContainer<TwoDBarcodedTube> vesselContainer = new VesselContainer<TwoDBarcodedTube>(this);

    protected TubeFormation() {
    }

    public TubeFormation(Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube, RackOfTubes.RackType rackType) {
        super(makeDigest(mapPositionToTube));
        this.rackType = rackType;
        for (Map.Entry<VesselPosition, TwoDBarcodedTube> vesselPositionTwoDBarcodedTubeEntry : mapPositionToTube.entrySet()) {
            vesselContainer.addContainedVessel(vesselPositionTwoDBarcodedTubeEntry.getValue(), vesselPositionTwoDBarcodedTubeEntry.getKey());
        }
    }

    /**
     * Make a digest from position / tube pairs
     * @param mapPositionToTube each entry is a position / tube pair
     * @return digest
     */
    private static String makeDigest(Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube) {
        List<Map.Entry<VesselPosition, String>> positionBarcodeList = new ArrayList<Map.Entry<VesselPosition, String>>();
        for (Map.Entry<VesselPosition, TwoDBarcodedTube> vesselPositionTwoDBarcodedTubeEntry : mapPositionToTube.entrySet()) {
            positionBarcodeList.add(new AbstractMap.SimpleEntry<VesselPosition, String>(
                    vesselPositionTwoDBarcodedTubeEntry.getKey(), vesselPositionTwoDBarcodedTubeEntry.getValue().getLabel()));
        }

        return makeDigest(positionBarcodeList);
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return rackType.getVesselGeometry();
    }

    public RackOfTubes.RackType getRackType() {
        return rackType;
    }

    /**
     * Make a digest from a list of position / tube barcode pairs
     * @param positionBarcodeList pairs of position / tube barcode
     * @return digest
     */
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

    /**
     * Makes a digest of a String holding the positions and barcodes of a set of tubes.  The digest is used to
     * refer to the formation, e.g. to retrieve existing formation from the database
     * @param positionBarcodeTupleString a sequence of position / barcode pairs, in alphabetical order of position
     * @return digest
     */
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
    public ContainerType getType() {
        return ContainerType.TUBE_FORMATION;
    }

    @Override
    public VesselContainer<TwoDBarcodedTube> getContainerRole() {
        return vesselContainer;
    }

    public void setVesselContainer(VesselContainer<TwoDBarcodedTube> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }

    public String getDigest() {
        return getLabel();
    }

    public void addRackOfTubes(RackOfTubes rackOfTubes) {
        racksOfTubes.add(rackOfTubes);
    }

    public Set<RackOfTubes> getRacksOfTubes() {
        return racksOfTubes;
    }

    @Override
    public String getLabCentricName() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (RackOfTubes rack : racksOfTubes) {
            if (first) {
                first = false;
            } else {
                builder.append(" ");
            }
            builder.append(rack.getLabel());
        }
        return builder.toString();
    }
}
