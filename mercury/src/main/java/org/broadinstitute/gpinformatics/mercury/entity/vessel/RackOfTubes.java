package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A rack of tubes
 */
@Entity
@Audited
@Table(schema = "mercury")
public class RackOfTubes extends LabVessel implements VesselContainerEmbedder<TwoDBarcodedTube> {

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<RackOfTubes2> rackOfTubes2s = new HashSet<RackOfTubes2>();

    @Enumerated(EnumType.STRING)
    private RackOfTubes2.RackType rackType;

    @Embedded
    private VesselContainer<TwoDBarcodedTube> vesselContainer = new VesselContainer<TwoDBarcodedTube>(this);

    RackOfTubes() {
    }

    public RackOfTubes(Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube, RackOfTubes2.RackType rackType) {
        super(makeDigest(mapPositionToTube));
        this.rackType = rackType;
        for (Map.Entry<VesselPosition, TwoDBarcodedTube> vesselPositionTwoDBarcodedTubeEntry : mapPositionToTube.entrySet()) {
            vesselContainer.addContainedVessel(vesselPositionTwoDBarcodedTubeEntry.getValue(), vesselPositionTwoDBarcodedTubeEntry.getKey());
        }
    }

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

    public RackOfTubes2.RackType getRackType() {
        return rackType;
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
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.RACK_OF_TUBES;
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

    public void addRackOfTubes(RackOfTubes2 rackOfTubes) {
        rackOfTubes2s.add(rackOfTubes);
    }

    public Set<RackOfTubes2> getRackOfTubes2s() {
        return rackOfTubes2s;
    }
}
