package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of tubes in a particular combination of positions, typically held in a RackOfTubes.  This entity is different
 * from RackOfTubes, because racks can be reused to hold different sets of tubes.
 */
@Entity
@Audited
public class TubeFormation extends LabVessel implements VesselContainerEmbedder<BarcodedTube> {

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury", name = "LAB_VESSEL_RACKS_OF_TUBES",
                      joinColumns = @JoinColumn(name = "LAB_VESSEL"),
                      inverseJoinColumns = @JoinColumn(name = "RACKS_OF_TUBES"))
    private Set<RackOfTubes> racksOfTubes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private RackOfTubes.RackType rackType;

    @Embedded
    private VesselContainer<BarcodedTube> vesselContainer = new VesselContainer<>(this);

    protected TubeFormation() {
    }

    public TubeFormation(Map<VesselPosition, BarcodedTube> mapPositionToTube, RackOfTubes.RackType rackType) {
        super(makeDigest(mapPositionToTube));
        this.rackType = rackType;
        for (Map.Entry<VesselPosition, BarcodedTube> vesselPositionBarcodedTubeEntry : mapPositionToTube.entrySet()) {
            vesselContainer.addContainedVessel(vesselPositionBarcodedTubeEntry.getValue(), vesselPositionBarcodedTubeEntry.getKey());
        }
    }

    /**
     * Make a digest from position / tube pairs
     * @param mapPositionToTube each entry is a position / tube pair
     * @return digest
     */
    static String makeDigest(Map<VesselPosition, BarcodedTube> mapPositionToTube) {
        List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
        for (Map.Entry<VesselPosition, BarcodedTube> vesselPositionBarcodedTubeEntry : mapPositionToTube.entrySet()) {
            positionBarcodeList.add(new ImmutablePair<>(vesselPositionBarcodedTubeEntry.getKey(),
                    vesselPositionBarcodedTubeEntry.getValue().getLabel()));
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
    public static String makeDigest(List<Pair<VesselPosition, String>> positionBarcodeList) {
        Collections.sort(positionBarcodeList, new Comparator<Pair<VesselPosition, String>>() {
            @Override
            public int compare(Pair<VesselPosition, String> o1, Pair<VesselPosition, String> o2) {
                return o1.getLeft().compareTo(o2.getLeft());
            }
        });
        StringBuilder stringBuilder = new StringBuilder();
        for (Pair<VesselPosition, String> positionBarcodeEntry : positionBarcodeList) {
            stringBuilder.append(positionBarcodeEntry.getLeft());
            stringBuilder.append(positionBarcodeEntry.getRight());
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
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ContainerType getType() {
        return ContainerType.TUBE_FORMATION;
    }

    @Override
    public VesselContainer<BarcodedTube> getContainerRole() {
        return vesselContainer;
    }

    public void setVesselContainer(VesselContainer<BarcodedTube> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }

    public String getDigest() {
        return getLabel();
    }

    public void addRackOfTubes(RackOfTubes rackOfTubes) {
        racksOfTubes.add(rackOfTubes);
        rackOfTubes.getTubeFormations().add(this);
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
