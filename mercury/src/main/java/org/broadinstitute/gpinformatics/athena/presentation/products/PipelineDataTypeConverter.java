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

package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PipelineDataTypeDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import java.util.Collection;
import java.util.Locale;

/**
 * This class is used by the Stripes Validation system for converting from String to PipelineDataType.
 *
 * @see net.sourceforge.stripes.validation.TypeConverter
 */
public class PipelineDataTypeConverter implements TypeConverter<PipelineDataType> {

    private PipelineDataTypeDao pipelineDataTypeDao = ServiceAccessUtility.getBean(PipelineDataTypeDao.class);

    @Override
    public void setLocale(Locale locale) {

    }

    @Override
    public PipelineDataType convert(String input, Class<? extends PipelineDataType> targetType,
                                    Collection<ValidationError> errors) {
        PipelineDataType pipelineDataType = null;
        try {
            pipelineDataType = pipelineDataTypeDao.findDataType(input);
        } catch (Exception e) {
            errors.add(new SimpleError("Error converting data type from {1}: {2}", e.getLocalizedMessage()));
        }

        return pipelineDataType;
    }

}
