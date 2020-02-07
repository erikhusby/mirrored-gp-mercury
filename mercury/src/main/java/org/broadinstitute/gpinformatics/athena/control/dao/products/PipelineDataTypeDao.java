/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class PipelineDataTypeDao extends GenericDao implements Serializable {
    private static final long serialVersionUID = -5509302444273367564L;

    public List<PipelineDataType> findActive() {
        return findList(PipelineDataType.class, PipelineDataType_.active, true);
    }

    public List<PipelineDataType> findAll() {
        return findAll(PipelineDataType.class);
    }

    public List<String> findAllActiveDataTypeNames() {
        return Optional.ofNullable(findAll()).orElse(Collections.emptyList()).stream().filter(PipelineDataType::isActive)
            .map(PipelineDataType::getName)
            .collect(Collectors.toList());
    }
    public PipelineDataType findDataType(String aggregationDataType) {
        return findSingle(PipelineDataType.class, PipelineDataType_.name, aggregationDataType);
    }
}
