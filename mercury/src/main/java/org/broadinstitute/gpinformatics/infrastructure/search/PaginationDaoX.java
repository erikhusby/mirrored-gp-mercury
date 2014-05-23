/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.beanutils.PropertyUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ejb.HibernateEntityManager;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds methods for pagination of criteria results. The start method projects the ID for
 * the entity out of the criteria, to get a list of IDs. The page method then retrieves
 * entities for a subset of the list of IDs.
 */
public class PaginationDaoX extends GenericDao {
    /**
     * Holds the current location in a pagination sequence, intended to be placed in HTTP
     * session.
     */
    public static class Pagination implements Serializable {

        private static final long serialVersionUID = -2657109310254480805L;

        private final int pageSize;

        private List<?> idList;

        private List<String> joinFetchPaths = new ArrayList<>();

        private String resultEntity;

        private String resultEntityId;

        public Integer getNumberPages() {
            if (idList == null) {
                return null;
            }
            return Math.round((float) Math.ceil(idList.size() / (float) pageSize));
        }

        public Pagination(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getPageSize() {
            return pageSize;
        }

        public List<?> getIdList() {
            return idList;
        }

        public void setIdList(List<?> idList) {
            this.idList = idList;
        }

        public List<String> getJoinFetchPaths() {
            return joinFetchPaths;
        }

        public void setJoinFetchPaths(List<String> joinFetchPaths) {
            this.joinFetchPaths = joinFetchPaths;
        }

        public String getResultEntity() {
            return resultEntity;
        }

        public void setResultEntity(String resultEntity) {
            this.resultEntity = resultEntity;
        }

        public String getResultEntityId() {
            return resultEntityId;
        }

        public void setResultEntityId(String resultEntityId) {
            this.resultEntityId = resultEntityId;
        }
    }

    /**
     * Get the list of IDs for the criteria
     *
     * @param criteria   criteria for which results are to be paginated
     * @param pagination holds pagination state
     */
    public void startPagination(Criteria criteria, Pagination pagination) {
        // Always append entity ID to order, because other less-specific orders may not
        // yield reproducible results.

        criteria.addOrder(Order.asc(pagination.getResultEntityId()));

        criteria.setProjection(Projections.property(pagination.getResultEntityId()));
        //noinspection unchecked
        pagination.setIdList(criteria.list());
    }

    /**
     * Gets one page out of the total set of results
     *
     * @param pagination holds IDs and page size
     * @param pageNumber number of requested page, starting at zero
     * @return list of entities for requested page, in same order as IDs
     */
    public <T> List<T> getPage(PaginationDaoX.Pagination pagination, int pageNumber) {
        if (pagination.getIdList().isEmpty()) {
            return Collections.emptyList();
        }
        List<?> idSubList = pagination.getIdList().subList(pageNumber * pagination.getPageSize(),
                Math.min((pageNumber + 1) * pagination.getPageSize(), pagination.getIdList().size()));

        @SuppressWarnings("unchecked")
        List<T> entityList = (List<T>) buildCriteria(pagination, idSubList).list();
        return reorderList(pagination, idSubList, entityList);
    }

    /**
     * Builds criteria to retrieve entities with given IDs
     *
     * @param pagination contains metadata about the entity
     * @param idList     list of primary keys
     * @return criteria
     */
    private Criteria buildCriteria(Pagination pagination, List<?> idList) {
        HibernateEntityManager hibernateEntityManager = getEntityManager().unwrap(HibernateEntityManager.class);
        Session hibernateSession = hibernateEntityManager.getSession();
        Criteria criteria = hibernateSession.createCriteria(pagination.getResultEntity());
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        // Improve performance, by reducing the number of selects
        for (String joinFetchPath : pagination.getJoinFetchPaths()) {
            String[] criteriaSteps = joinFetchPath.split("\\.");
            for (String criteriaStep : criteriaSteps) {
                criteria.setFetchMode(criteriaStep, FetchMode.JOIN);
            }
        }
        // OR together IN clauses to avoid hitting the Oracle limit. See SQLTools.IN_QUERY_BATCH_SIZE
        Criterion criterion = null;
        for (Collection<?> idSubList : BaseSplitter.split(idList)) {
            Criterion in = Restrictions.in(pagination.getResultEntityId(), idSubList);
            criterion = criterion != null ? Restrictions.or(criterion, in) : in;
        }
        criteria.add(criterion);
        return criteria;
    }

    /**
     * Oracle may return the IDs in an arbitrary order, so reorder them to match the list
     * of IDs
     *
     * @param pagination contains metadata about the entity
     * @param idList     list of primary keys
     * @param entityList list of retrieved entities
     * @return list of entities, in same order as idList
     */
    private static <T> List<T> reorderList(Pagination pagination, List<?> idList, List<T> entityList) {
        // re-order results to match order of input IDs list
//        Object parsed;
        try {
//            parsed = Ognl.parseExpression(pagination.getResultEntityId());

            Map<Object, T> criteriaIdMap = new HashMap<>();
            List<T> returnList = new ArrayList<>();
            for (T entity : entityList) {
//                Object id = Ognl.getValue(parsed, entity);
                Object id = PropertyUtils.getProperty(entity, pagination.getResultEntityId());
                criteriaIdMap.put(id, entity);
            }
            for (Object id : idList) {
                returnList.add(criteriaIdMap.get(id));
            }
            return returnList;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The user interface allows a user to check multiple boxes next to result rows
     *
     * @param pagination information about entity and ID property
     * @param idList     ids the user wants to access
     * @return list of entities, in same order as idList
     */
    public <T> List<T> getByIds(PaginationDaoX.Pagination pagination, List<?> idList) {
        @SuppressWarnings("unchecked")
        List<T> entityList = buildCriteria(pagination, idList).list();
        return reorderList(pagination, idList, entityList);
    }
}
