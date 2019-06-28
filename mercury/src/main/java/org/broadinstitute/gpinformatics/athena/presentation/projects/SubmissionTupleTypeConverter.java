/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;

import java.util.Collection;
import java.util.Locale;

public class SubmissionTupleTypeConverter implements TypeConverter<SubmissionTuple> {
    @Override
    public void setLocale(Locale locale) {

    }

    @Override
    public SubmissionTuple convert(String input, Class<? extends SubmissionTuple> targetType,
                                   Collection<ValidationError> errors) {
        return new SubmissionTuple(input);
    }
}
