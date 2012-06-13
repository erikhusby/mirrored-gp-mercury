package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// todo jmt rename to TubeFormation, create separate (non-LabVessel) class for rack
/**
 * A rack of tubes
 */
@NamedQueries({
        @NamedQuery(
                name = "RackOfTubes.fetchByDigest",
                query = "select r from RackOfTubes r where digest = :digest"
        ),
        @NamedQuery(
                name = "RackOfTubes.fetchByLabel",
                query = "select r from RackOfTubes r where label = :label"
        )
})
@Entity
public class RackOfTubes extends LabVessel implements SBSSectionable, VesselContainerEmbedder<TwoDBarcodedTube> {

    // todo jmt can't make this non-null, because all LabVessels subtypes are in the same table
    // todo jmt unique constraint?
//    @Column(nullable = false)
    private String digest;

    @Embedded
    private VesselContainer<TwoDBarcodedTube> vesselContainer = new VesselContainer<TwoDBarcodedTube>(this);

    public RackOfTubes(String label) {
        super(label);
    }

    protected RackOfTubes() {
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
    public SBSSection getSection() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return this.getVesselContainer().getSampleInstances();
    }

    @Override
    public Collection<Project> getAllProjects() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public StatusNote getLatestNote() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void logNote(StatusNote statusNote) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        throw new RuntimeException("I haven't been written yet.");
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

    public VesselContainer<TwoDBarcodedTube> getVesselContainer() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<TwoDBarcodedTube> vesselContainer) {
        this.vesselContainer = vesselContainer;
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
