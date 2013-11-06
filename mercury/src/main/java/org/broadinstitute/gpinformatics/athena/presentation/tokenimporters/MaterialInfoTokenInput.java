package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialInfoList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Token Input support for Material Info.
 *
 * @author hrafal
 */
public class MaterialInfoTokenInput extends TokenInput<MaterialInfo> {

    @Inject
    private BSPMaterialInfoList materialInfoList;

    public MaterialInfoTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected MaterialInfo getById(String name) {
        return materialInfoList.getByFullName(name);
    }


    public String getJsonString(String query) throws JSONException {

        if (materialInfoList == null) {
            return "";
        }

        Collection<MaterialInfo> materialInfos = materialInfoList.find(query);
        return createItemListString(new ArrayList<>(materialInfos));
    }

    @Override
    protected String getTokenId(MaterialInfo materialInfo) {
        return materialInfo.getFullName();
    }

    @Override
    protected String formatMessage(String messageString, MaterialInfo materialInfo) {
        return MessageFormat.format(messageString, materialInfo.getFullName());
    }

    @Override
    protected String getTokenName(MaterialInfo materialInfo) {
        return materialInfo.getFullName();
    }

    @Override
    public JSONObject createAutocomplete(MaterialInfo materialInfo) throws JSONException {
        if (materialInfo == null) {
            return null;
        }

        return super.createAutocomplete(materialInfo);
    }
}
