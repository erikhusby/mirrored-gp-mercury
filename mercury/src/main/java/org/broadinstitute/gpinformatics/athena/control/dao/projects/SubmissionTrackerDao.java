package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker_;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.submission.ISubmissionTuple;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
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
     *     <li>Research Project (Squid)</li>
     *     <li>Sample Name</li>
     *     <li>File Type</li>
     *     <li>Processing Location</li>
     *     <li>File Version</li>
     * </ul>
     * @param tupleCollection
     */
    public List<SubmissionTracker> findSubmissionTrackers(Collection<? extends ISubmissionTuple> tupleCollection) {
        CriteriaBuilder submissionTrackerCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<SubmissionTracker> criteriaQuery = getCriteriaBuilder().createQuery(SubmissionTracker.class);
        Root<SubmissionTracker> root = criteriaQuery.from(SubmissionTracker.class);

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


    @Deprecated
    public List<SubmissionTracker> findTrackersMissingDatatypeAndLocation(){
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<SubmissionTracker> criteriaQuery = criteriaBuilder.createQuery(SubmissionTracker.class);
        Root<SubmissionTracker> root = criteriaQuery.from(SubmissionTracker.class);
        criteriaQuery.where(
            criteriaBuilder.and(
                criteriaBuilder.isNull(root.get(SubmissionTracker_.dataType)),
                criteriaBuilder.isNull(root.get(SubmissionTracker_.processingLocation))
            ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(SubmissionTracker_.requestDate)));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public List<SubmissionTracker> findTrackersMissingDatatypeOrLocation(){
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<SubmissionTracker> criteriaQuery = criteriaBuilder.createQuery(SubmissionTracker.class);
        Root<SubmissionTracker> root = criteriaQuery.from(SubmissionTracker.class);
        criteriaQuery.where(
            criteriaBuilder.or(
                criteriaBuilder.isNull(root.get(SubmissionTracker_.dataType)),
                criteriaBuilder.isNull(root.get(SubmissionTracker_.processingLocation))
            ));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(SubmissionTracker_.requestDate)));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }
}
