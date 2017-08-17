package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker_;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.submission.ISubmissionTuple;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class SubmissionTrackerDao extends GenericDao {

    /**
     * Find any submissionTrackers based on their:
     * <ul>
     *     <li>Research Project Jira Key</li>
     *     <li>Sample Name</li>
     *     <li>File Type</li>
     *     <li>Processing Location</li>
     *     <li>File Version</li>
     * </ul>
     * @param tupleCollection
     * @return
     */
    public List<SubmissionTracker> findSubmissionTrackers(Collection<? extends ISubmissionTuple> tupleCollection) {
        CriteriaBuilder submissionTrackerCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<SubmissionTracker> criteriaQuery = getCriteriaBuilder().createQuery(SubmissionTracker.class);
        Root<SubmissionTracker> root = criteriaQuery.from(SubmissionTracker.class);
        Join<SubmissionTracker, ResearchProject> researchProjectJoin = root.join(SubmissionTracker_.researchProject);

        Collection<Predicate> predicates = new HashSet<>(tupleCollection.size());
        for (ISubmissionTuple submissionObject : tupleCollection) {
            SubmissionTuple submissionTuple = submissionObject.getSubmissionTuple();
            predicates.add(submissionTrackerCriteria.and(
                submissionTrackerCriteria.equal(root.get(SubmissionTracker_.project), submissionTuple.getProject()),
                submissionTrackerCriteria
                    .equal(root.get(SubmissionTracker_.submittedSampleName), submissionTuple.getSampleName()),
                submissionTrackerCriteria.or(
                    submissionTrackerCriteria.equal(root.get(SubmissionTracker_.dataType), submissionTuple.getDataType()),
                    submissionTrackerCriteria.isNull(root.get(SubmissionTracker_.dataType))
                ),
                submissionTrackerCriteria.or(
                    submissionTrackerCriteria.equal(root.get(SubmissionTracker_.processingLocation),
                        submissionTuple.getProcessingLocation()),

                    // Until processing location gets back-filled.
                    submissionTrackerCriteria.isNull(root.get(SubmissionTracker_.processingLocation))),
                submissionTrackerCriteria.equal(root.get(SubmissionTracker_.fileType), submissionTuple.getFileType())
            ));
        }
        Predicate orPredicate = submissionTrackerCriteria.or(predicates.toArray(new Predicate[predicates.size()]));
        criteriaQuery.where(orPredicate);

        return getEntityManager().createQuery(criteriaQuery).getResultList();
    }

    @Override
    public void persist(Object entity) {
        persistAll(Collections.singletonList(entity));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void persistAll(Collection<?> entities) {
        Collection<SubmissionTracker> trackers = (Collection<SubmissionTracker>) entities;
        List<SubmissionTracker> foundTrackers = findSubmissionTrackers(trackers);
        if (!foundTrackers.isEmpty()) {
            List<String> errors = new ArrayList<>(foundTrackers.size());
            for (SubmissionTracker foundTracker : foundTrackers) {
                if (SubmissionTuple.hasTuple((List<? extends ISubmissionTuple>) trackers, foundTracker)) {
                    errors.add(foundTracker.getSubmissionTuple().toString());
                }
            }

            if (!errors.isEmpty()) {
                throw new RuntimeException(String.format("Submission(s) already exists for (%s).", errors));
            }
        }
        super.persistAll(entities);
    }
}
