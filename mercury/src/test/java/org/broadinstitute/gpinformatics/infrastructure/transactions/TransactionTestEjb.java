package org.broadinstitute.gpinformatics.infrastructure.transactions;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;

/**
 */
@Stateful
@RequestScoped
public class TransactionTestEjb {

    @Inject
    private VanillaDao dao;

    public void doNothing() {
    }

    public void doSomething() {
        dao.findById(ResearchProject.class, 1L);
    }

    public void updateVolume(StaticPlate plate, BigDecimal volume) {
        plate.setVolume(volume);
    }

    public void updateVolumeWithFind(Long id, BigDecimal volume) {
        StaticPlate plate = dao.findById(StaticPlate.class, id);
        plate.setVolume(volume);
    }

    public void updateVolumeWithPersist(StaticPlate plate, BigDecimal volume) {
        plate.setVolume(volume);
        dao.persist(plate);
    }

    public void updateVolumeWithUnrelatedFind(StaticPlate plate, BigDecimal volume) {
        dao.findById(ResearchProject.class, 1L);
        plate.setVolume(volume);
    }
}
