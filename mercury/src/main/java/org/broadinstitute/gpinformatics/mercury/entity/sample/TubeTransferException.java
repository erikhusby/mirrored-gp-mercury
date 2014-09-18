package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import javax.annotation.Nonnull;
import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class TubeTransferException extends RuntimeException {

    private static final Log logger = LogFactory.getLog(TubeTransferException.class);

    private static final long serialVersionUID = 1355836331464448642L;

    public TubeTransferException(@Nonnull ManifestRecord.ErrorStatus errorStatus, @Nonnull Metadata.Key metaDataType,
                                 @Nonnull String metaDataValue) {
        this(errorStatus, metaDataType, metaDataValue, "");
    }

    public TubeTransferException(@Nonnull ManifestRecord.ErrorStatus errorStatus, @Nonnull Metadata.Key metaDataType,
                                 @Nonnull String metaDataValue, @Nonnull String message) {
        this(errorStatus, metaDataType.getDisplayName(), metaDataValue, message);
    }

    public TubeTransferException(ManifestRecord.ErrorStatus errorStatus, String dataType,
                                 String dataValue, String message) {

        super(errorStatus.formatMessage(dataType, dataValue) + (StringUtils.isBlank(message) ? "": " " + message));
        logger.error(getMessage());
    }
}
