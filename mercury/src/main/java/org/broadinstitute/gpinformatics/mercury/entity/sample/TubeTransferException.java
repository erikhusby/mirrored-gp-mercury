package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class TubeTransferException extends RuntimeException {

    private static final Log logger = LogFactory.getLog(TubeTransferException.class);


    private static final long serialVersionUID = 1355836331464448642L;
    private ManifestRecord.ErrorStatus errorStatus;

    public TubeTransferException(@Nonnull ManifestRecord.ErrorStatus errorStatus, @Nonnull String metaDataType,
                                 @Nonnull String metaDataValue, @Nonnull Throwable cause) {
        super(errorStatus.formatMessage(metaDataType, metaDataValue), cause);
        this.errorStatus = errorStatus;
        logger.error(getMessage());
    }


    public TubeTransferException(@Nonnull ManifestRecord.ErrorStatus errorStatus, @Nonnull String metaDataType,
                                 @Nonnull String metaDataValue) {
        this(errorStatus, metaDataType, metaDataValue, "");
    }

    public TubeTransferException(@Nonnull ManifestRecord.ErrorStatus errorStatus, @Nonnull String metaDataType,
                                 @Nonnull String metaDataValue, @Nonnull String message) {
        super(StringUtils.trim(errorStatus.formatMessage(metaDataType, metaDataValue) + " " + message));
        this.errorStatus = errorStatus;
        logger.error(getMessage());
    }

    public ManifestRecord.ErrorStatus getErrorStatus() {
        return errorStatus;
    }
}
