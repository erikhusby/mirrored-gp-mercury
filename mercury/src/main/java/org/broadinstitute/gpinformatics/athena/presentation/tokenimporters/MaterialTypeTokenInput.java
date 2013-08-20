package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Token Input support for Material Type.
 *
 * @author hrafal
 */
public class MaterialTypeTokenInput extends TokenInput<MaterialType> {

    @Inject
    private BSPMaterialTypeList materialTypeListCache;

    public MaterialTypeTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected MaterialType getById(String name) {
        return materialTypeListCache.getByFullName(name);
    }

    public String getJsonString(String query) throws JSONException {

        if (materialTypeListCache == null) {
            return "";
        }

        Collection<MaterialType> materialTypes = materialTypeListCache.find(query);
        return createItemListString(new ArrayList<>(materialTypes));
    }

    @Override
    protected String getTokenId(MaterialType materialType) {
        return materialType.getFullName();
    }

    @Override
    protected String formatMessage(String messageString, MaterialType materialType) {
        return MessageFormat.format(messageString, materialType.getFullName());
    }

    @Override
    protected String getTokenName(MaterialType materialType) {
        return materialType.getFullName();
    }

    @Override
    public JSONObject createAutocomplete(MaterialType materialType) throws JSONException {
        if (materialType == null) {
            return null;
        }

        return super.createAutocomplete(materialType);
    }

    public Collection<? extends org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType> getMercuryTokenObjects() {
        List<org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType> mercuryTokenObjects =
                new ArrayList<>();

        for (MaterialType materialType : getTokenObjects()) {
            if (materialType != null) {
                org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType mercuryMaterialType =
                        new org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType(materialType.getCategory(), materialType.getName());

                mercuryTokenObjects.add(mercuryMaterialType);
            }
        }

        return mercuryTokenObjects;
    }
}
