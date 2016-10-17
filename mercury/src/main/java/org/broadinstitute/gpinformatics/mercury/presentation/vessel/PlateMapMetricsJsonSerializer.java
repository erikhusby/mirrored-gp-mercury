package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

public class PlateMapMetricsJsonSerializer extends JsonSerializer<MetricsViewActionBean.PlateMapMetrics> {
    @Override
    public void serialize(MetricsViewActionBean.PlateMapMetrics value, JsonGenerator generator,
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
