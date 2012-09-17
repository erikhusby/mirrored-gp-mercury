package org.broadinstitute.sequel.presentation.pass;


public class PassSample {


        /**
         * Minimum 100ng = 0.1&mu;g
         */
        public static final Double MIN_ERR_TOTAL_DNA_NG = 100.0;

        /**
         * Minimum 500ng = 0.5&mu;g
         */
        public static final Double MIN_WARN_TOTAL_DNA_NG = 500.0;

        /**
         * Minimum concentration 0.2 ng/&mu;L
         */
        public static final Double MIN_ERR_CONC_NG_UL = 0.2;


        private String sampleId;

        private String participantId;

        private String collaboratorSampleId;

        private String collaboratorParticipantId;

        private String materialType;

        private String volume;

        private String concentration;

        private String totalDNA;

        private String sampleType;

        private String primaryDisease;

        private String gender;

        private String stockType;

        private boolean fingerprinted;

        private String note;

        private Long sampleIndex;

        private boolean lookupError;

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public String getCollaboratorSampleId() {
            return collaboratorSampleId;
        }

        public void setCollaboratorSampleId(String collaboratorSampleId) {
            this.collaboratorSampleId = collaboratorSampleId;
        }

        public String getCollaboratorParticipantId() {
            return collaboratorParticipantId;
        }

        public void setCollaboratorParticipantId(String collaboratorParticipantId) {
            this.collaboratorParticipantId = collaboratorParticipantId;
        }

        public String getMaterialType() {
            return materialType;
        }

        public void setMaterialType(String materialType) {
            this.materialType = materialType;
        }

        public String getVolume() {
            return volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public String getConcentration() {
            return concentration;
        }

        public void setConcentration(String concentration) {
            this.concentration = concentration;
        }

        public String getSampleType() {
            return sampleType;
        }

        public void setSampleType(String sampleType) {
            this.sampleType = sampleType;
        }

        public String getPrimaryDisease() {
            return primaryDisease;
        }

        public void setPrimaryDisease(String primaryDisease) {
            this.primaryDisease = primaryDisease;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getStockType() {
            return stockType;
        }

        public void setStockType(String stockType) {
            this.stockType = stockType;
        }

        public boolean isFingerprinted() {
            return fingerprinted;
        }

        public void setFingerprinted(boolean fingerprinted) {
            this.fingerprinted = fingerprinted;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getNote() {
            return note;
        }

        public void setTotalDNA(String totalDNA) {
            this.totalDNA = totalDNA;
        }

        public String getTotalDNA() {
            return totalDNA;
        }

        public boolean isTotalDNAError() {
            return Double.valueOf(getTotalDNA()) < MIN_ERR_TOTAL_DNA_NG;
        }

        public boolean isTotalDNAWarning() {
            double val = Double.valueOf(getTotalDNA());
            return (MIN_ERR_TOTAL_DNA_NG <= val) && (val < MIN_WARN_TOTAL_DNA_NG);
        }

        public boolean isNoFingerprintWarning() {
            return !isFingerprinted();
        }

        public boolean isConcentrationError() {
            return Double.valueOf(getConcentration()) < MIN_ERR_CONC_NG_UL;
        }


        public boolean isError() {
            return isLookupError() || isTotalDNAError() || isConcentrationError();
        }

        public boolean isWarning() {
            return isTotalDNAWarning() || isNoFingerprintWarning();
        }

        public Long getSampleIndex() {
            return this.sampleIndex;
        }

        public void setSampleIndex(Long sampleIndex) {
            this.sampleIndex = sampleIndex;
        }

        public void setLookupError(boolean lookupError) {
            this.lookupError = lookupError;
        }

        public boolean isLookupError() {
            return lookupError;
        }

        public String getErrorOrWarningText() {
            if (isLookupError()) {
                return "BSP LOOKUP";
            }
            if (isTotalDNAError()) {
                return "TOTAL DNA";
            }
            if (isConcentrationError()) {
                return "CONC";
            }
            if (isTotalDNAWarning()) {
                return "TOTAL DNA";
            }
            if (isNoFingerprintWarning()) {
                return "FINGERPRINT";
            }
            return "";

        }


    }
