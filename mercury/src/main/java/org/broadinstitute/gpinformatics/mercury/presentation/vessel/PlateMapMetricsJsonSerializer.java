package org.broadinstitute.gpinformatics.mercury.presentation.vessel;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PlateMapMetricsJsonSerializer extends JsonSerializer<MetricsViewActionBean.DisplayMetrics> {
    @Override
    public void serialize(MetricsViewActionBean.DisplayMetrics value, JsonGenerator generator,
                          SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("displayName");
        generator.writeString(value.getDisplayName());
        generator.writeFieldName("displayValue");
        generator.writeBoolean(value.isDisplayValue());
        generator.writeFieldName("chartType");
        generator.writeString(value.getChartType().name());
        generator.writeFieldName("evalType");
        generator.writeString(value.getEvalType());
        generator.writeEndObject();
    }
}
