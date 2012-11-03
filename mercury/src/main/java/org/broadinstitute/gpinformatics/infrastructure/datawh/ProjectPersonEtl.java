package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.mercury.control.dao.person.PersonDAO;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ProjectPersonEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

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
        if (entity == null) return null;

        Long personId = entity.getPersonId();
        if (personId == null) return null;

        Person person = dao.getEntityManager().find(Person.class, personId);

        return genericRecord(etlDateStr, false,
                entity.getProjectPersonId(),
                format(entity.getResearchProject().getResearchProjectId()),
                format(entity.getRole().toString()),
                format(entity.getPersonId()),
                format(person.getFirstName()),
                format(person.getLastName()),
                format(person.getLogin()));
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
