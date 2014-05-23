package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * Created by thompson on 5/22/2014.
 */
public enum ColumnEntity {
    LAB_VESSEL(new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((LabVessel) entity).getLabel();
        }
    });

    ColumnEntity(IdGetter idGetter) {
        this.idGetter = idGetter;
    }

    public interface IdGetter {
        String getId(Object entity);
    }

    private IdGetter idGetter;

    public IdGetter getIdGetter() {
        return idGetter;
    }
}
