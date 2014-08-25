package org.broadinstitute.gpinformatics.mercury.entity.sample;

import clover.com.google.common.base.Function;
import clover.com.google.common.collect.Maps;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.EnumType;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ManifestRecord {


    private Map<Metadata.Key,Metadata> metadata = new HashMap<>();

    public ManifestRecord(List<Metadata> metadata) {

        this.metadata = new HashMap<>(Maps.uniqueIndex(metadata, new Function<Metadata, Metadata.Key>() {
            @Override
            public Metadata.Key apply(@Nullable Metadata metadata) {
                return metadata.getKey();
            }
        }));
    }

    public Metadata getField(Metadata.Key sampleId) {


        return metadata.get(sampleId);
    }
}
