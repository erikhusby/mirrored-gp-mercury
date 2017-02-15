package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

public enum LimsFileType implements Displayable {
    QIAGEN_BLOOD_BIOPSY_24("Qiagen Blood Biopsy 24 Sample Carriers", ExpectSourcesFromFile.TRUE, ExpectTargetsFromFile.TRUE);

    private final String displayName;
    private final ExpectSourcesFromFile expectSourcesFromFile;
    private final ExpectTargetsFromFile expectTargetsFromFile;

    LimsFileType(String displayName, ExpectSourcesFromFile expectSourcesFromFile,
                 ExpectTargetsFromFile expectTargetsFromFile) {
        this.displayName = displayName;
        this.expectSourcesFromFile = expectSourcesFromFile;
        this.expectTargetsFromFile = expectTargetsFromFile;
    }


    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Whether the sources should be found in the file
     */
    public enum ExpectSourcesFromFile {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        ExpectSourcesFromFile(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    /**
     * Whether the targets should be found in the file
     */
    public enum ExpectTargetsFromFile {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        ExpectTargetsFromFile(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }
}
