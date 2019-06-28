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

package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is the cohort implementation of the token object
 *
 * @author hrafal
 */
@Dependent
public class MaterialTypeTokenInput extends TokenInput<MaterialType> {
    public MaterialTypeTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected MaterialType getById(String materialType) {
        return MaterialType.valueOf(materialType);
    }

    public String getJsonString(String query) throws JSONException {
        List<String> searchTerms = new ArrayList<>();
        if (query != null) {
            searchTerms = Stream.of(query.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        }
        List<MaterialType> matches = new ArrayList<>();
        searchTerms.forEach(s -> MaterialType.stream()
            .filter(materialType -> materialType.containsIgnoringCase(s))
            .collect(Collectors.toCollection(() -> matches)));
        return createItemListString(matches);
    }

    @Override
    protected String getTokenId(MaterialType materialType) {
        return materialType.name();
    }

    @Override
    protected String getTokenName(MaterialType materialType) {
        return materialType.getDisplayName();
    }

    @Override
    protected String formatMessage(String messageString, MaterialType materialType) {
        return MessageFormat.format(messageString, materialType.getDisplayName());
    }
}
