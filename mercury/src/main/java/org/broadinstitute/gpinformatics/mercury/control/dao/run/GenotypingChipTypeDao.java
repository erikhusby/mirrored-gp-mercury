package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChipType;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChipType_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for GenotypingChipType
 */
@Stateful
@RequestScoped
public class GenotypingChipTypeDao extends GenericDao {

    /** Returns all the versions, sorted by created date desc, i.e. most recent is first. */
    public List<GenotypingChipType> findAllByName(String genotypingChipName) {
        List<GenotypingChipType> list = findList(GenotypingChipType.class, GenotypingChipType_.chipName, genotypingChipName);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        } else {
            Collections.sort(list, GenotypingChipType.BY_NAME_DATE_DESC);
            return list;
        }
    }

    /** Returns the most recent version of GenotypingChipType having the specified name, or null if none found. */
    public GenotypingChipType findByName(String genotypingChipName) {
        List<GenotypingChipType> list = findAllByName(genotypingChipName);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /** Returns the version of GenotypingChipType that was active as of effectiveDate, or null. */
    public GenotypingChipType findByName(String genotypingChipName, Date effectiveDate) {
        for (GenotypingChipType genotypingChipType : findAllByName(genotypingChipName)) {
            if (genotypingChipType.getCreatedDate().before(effectiveDate)) {
                return genotypingChipType;
            }
        }
        return null;
    }
}
