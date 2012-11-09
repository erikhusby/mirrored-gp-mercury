package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ProjectPersonEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    @Inject
    BSPUserList userList;

    @Override
    Class getEntityClass() {
        return ProjectPerson.class;
    }

    @Override
    String getBaseFilename() {
        return "research_project_person";
    }

    @Override
    Long entityId(Object entity) {
        return ((ProjectPerson)entity).getProjectPersonId();
    }

    /**
     * Makes a data record from selected entity fields, in a format that matches the corresponding
     * SqlLoader control file.
     * @param etlDateStr date
     * @param isDelete indicates deleted entity
     * @param entityId look up this entity
     * @return delimited SqlLoader record
     */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        ProjectPerson entity = dao.getEntityManager().find(ProjectPerson.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  ProjectPerson having id " + entityId + " no longer exists.");
            return null;
        }
        Long personId = entity.getPersonId();
        BspUser bspUser = null;
        if (personId != null) {
            bspUser = userList.getById(personId);
        }
        if (bspUser == null) {
            logger.info("Cannot export.  BspUser having id " + entityId + " no longer exists.");
            return null;
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getProjectPersonId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getRole() != null ? entity.getRole().toString() : null),
                format(entity.getPersonId()),
                format(bspUser.getFirstName()),
                format(bspUser.getLastName()),
                format(bspUser.getUsername())
        );
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity, boolean isDelete) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
