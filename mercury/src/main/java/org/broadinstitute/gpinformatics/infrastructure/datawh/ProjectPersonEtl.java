package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson_;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Stateless
public class ProjectPersonEtl  extends GenericEntityEtl {

    private ResearchProjectDao dao;
    private BSPUserList userList;

    @Inject
    public void setResearchProjectDao(ResearchProjectDao dao) {
	this.dao = dao;
    }

    @Inject
    public void setBSPUserList(BSPUserList userList) {
	this.userList = userList;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ProjectPerson.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "research_project_person";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ProjectPerson)entity).getProjectPersonId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        ProjectPerson entity = dao.findById(ProjectPerson.class, entityId);
        if (entity != null) {
	    recordList.add(entityRecord(etlDateStr, isDelete, entity));
	} else {
            logger.info("Cannot export. " + getEntityClass().getSimpleName() + " having id " + entityId + " no longer exists.");
        }
        return recordList;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<ProjectPerson> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<ProjectPerson>() {
                    @Override
                    public void callback(CriteriaQuery<ProjectPerson> cq, Root<ProjectPerson> root) {
                        CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                        cq.where(cb.between(root.get(ProjectPerson_.projectPersonId), startId, endId));
                    }
                });
        for (ProjectPerson entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ProjectPerson entity) {
        Long personId = entity.getPersonId();
        BspUser bspUser = null;
        if (personId != null) {
            bspUser = userList.getById(personId);
        }
        if (bspUser == null) {
            logger.info("Cannot export.  BspUser having id " + entity.getPersonId() + " no longer exists.");
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
