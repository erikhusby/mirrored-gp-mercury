package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
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
     *     <li>Sample Name</li>
     *     <li>File Type</li>
     *     <li>File Version</li>
     * </ul>
     * @param submissionDtos
     * @return
     */
    public List<SubmissionTracker> findSubmissionTrackers(Collection<SubmissionDto> submissionDtos) {
        CriteriaBuilder submissionTrackerCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<SubmissionTracker> criteriaQuery = getCriteriaBuilder().createQuery(SubmissionTracker.class);
        Root<SubmissionTracker> root = criteriaQuery.from(SubmissionTracker.class);
        Join<SubmissionTracker, ResearchProject> researchProjectJoin = root.join(SubmissionTracker_.researchProject);

        Collection<Predicate> predicates = new HashSet<>(submissionDtos.size());
        for (SubmissionDto submissionDto : submissionDtos) {
            predicates.add(submissionTrackerCriteria.and(
                    submissionTrackerCriteria.equal(root.get(SubmissionTracker_.project), submissionDto.getAggregationProject()),
                    submissionTrackerCriteria.equal(root.get(SubmissionTracker_.submittedSampleName), submissionDto.getSampleName()),
                    submissionTrackerCriteria.or(
                        submissionTrackerCriteria.equal(root.get(SubmissionTracker_.processingLocation),
                            submissionDto.getProcessingLocation()),

                        // Until processing location gets back-filled.
                        submissionTrackerCriteria.isNull(root.get(SubmissionTracker_.processingLocation))),
                    submissionTrackerCriteria.equal(root.get(SubmissionTracker_.fileType),submissionDto.getFileType())
                // todo: do we  need to join on version?
//                    submissionTrackerCriteria.equal(root.get(SubmissionTracker_.version), submissionDto.getVersion()),
            ));
        }
        Predicate orPredicate = submissionTrackerCriteria.or(predicates.toArray(new Predicate[predicates.size()]));
        criteriaQuery.where(orPredicate);

        return getEntityManager().createQuery(criteriaQuery).getResultList();
    }
}
