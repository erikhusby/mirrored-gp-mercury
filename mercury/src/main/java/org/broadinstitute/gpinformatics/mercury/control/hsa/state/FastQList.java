package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import javax.persistence.Column;
import java.io.File;

public class FastQList {
    @Column(name = "RGID")
    private String rgId;

    @Column(name = "RGSM")
    private String rgSm;

    @Column(name = "RGLB")
    private String rgLb;

    @Column(name = "Lane")
    private int lane;

    @Column(name = "Read1File")
    private String read1File;

    @Column(name = "Read2File")
    private String read2File;

    public FastQList() {
    }

    public String getRgId() {
        return rgId;
    }

    public void setRgId(String rgId) {
        this.rgId = rgId;
    }

    public String getRgSm() {
        return rgSm;
    }

    public void setRgSm(String rgSm) {
        this.rgSm = rgSm;
    }

    public String getRgLb() {
        return rgLb;
    }

    public void setRgLb(String rgLb) {
        this.rgLb = rgLb;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public String getRead1File() {
        return read1File;
    }

    public void setRead1File(String read1File) {
        this.read1File = read1File;
    }

    public String getRead2File() {
        return read2File;
    }

    public void setRead2File(String read2File) {
        this.read2File = read2File;
    }
}
