package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson_;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;

@Stateful
public class ProjectPersonEtl extends GenericEntityEtl<ProjectPerson, ProjectPerson> {
    private BSPUserList userList;

    public ProjectPersonEtl() {
        entityClass = ProjectPerson.class;
        baseFilename = "research_project_person";
    }

    @Inject
    public ProjectPersonEtl(ResearchProjectDao d, BSPUserList ul) {
        this();
        dao = d;
        userList = ul;
    }

    @Override
    Long entityId(ProjectPerson entity) {
        return entity.getProjectPersonId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(ProjectPerson_.projectPersonId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProjectPerson.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProjectPerson entity) {
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
}

