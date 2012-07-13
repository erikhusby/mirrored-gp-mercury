package org.broadinstitute.sequel.infrastructure.bsp.plating;

public interface Control {


    public static class Positive implements Control {

        private String sampleId;

        public Positive(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getSampleId() {
            return this.sampleId;
        }

        public boolean isPositive() {
            return true;
        }

    }

    public enum Negative implements Control {
        WATER_CONTROL {
            @Override
            public String getSampleId() {
                return "Sequencing Neg Control";
            }
        };

        public String getSampleId() {
            return this.name();
        }

        public boolean isPositive() {
            return false;
        }

        public String getPlatingQuote() {
            return null;
        }

    }


    // if a specific enum instance has a name that is not usable as a Java
    // identifier, override #getSampleId(). Otherwise #getSampleId on the enum
    // drops back to be #name(), which should be fine for most cases.
    String getSampleId();

    boolean isPositive();

}
