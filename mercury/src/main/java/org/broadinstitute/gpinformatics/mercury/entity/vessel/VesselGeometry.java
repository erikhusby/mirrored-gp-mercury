package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Row / column geometry for vessels
 */
public enum VesselGeometry {
    TUBE(
            "1x1",
            new String[]{""},
            new String[]{""}),

    WELL(
            "1x1",
            new String[]{""},
            new String[]{""}),

    G12x8(
            "12 x 8",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08, VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08, VesselPosition.B09, VesselPosition.B10, VesselPosition.B11, VesselPosition.B12,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08, VesselPosition.C09, VesselPosition.C10, VesselPosition.C11, VesselPosition.C12,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08, VesselPosition.D09, VesselPosition.D10, VesselPosition.D11, VesselPosition.D12,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04, VesselPosition.E05, VesselPosition.E06, VesselPosition.E07, VesselPosition.E08, VesselPosition.E09, VesselPosition.E10, VesselPosition.E11, VesselPosition.E12,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04, VesselPosition.F05, VesselPosition.F06, VesselPosition.F07, VesselPosition.F08, VesselPosition.F09, VesselPosition.F10, VesselPosition.F11, VesselPosition.F12,
                    VesselPosition.G01, VesselPosition.G02, VesselPosition.G03, VesselPosition.G04, VesselPosition.G05, VesselPosition.G06, VesselPosition.G07, VesselPosition.G08, VesselPosition.G09, VesselPosition.G10, VesselPosition.G11, VesselPosition.G12,
                    VesselPosition.H01, VesselPosition.H02, VesselPosition.H03, VesselPosition.H04, VesselPosition.H05, VesselPosition.H06, VesselPosition.H07, VesselPosition.H08, VesselPosition.H09, VesselPosition.H10, VesselPosition.H11, VesselPosition.H12
            }),

    G24x16(
            "24 x 16",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08, VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12, VesselPosition.A13, VesselPosition.A14, VesselPosition.A15, VesselPosition.A16, VesselPosition.A17, VesselPosition.A18, VesselPosition.A19, VesselPosition.A20, VesselPosition.A21, VesselPosition.A22, VesselPosition.A23, VesselPosition.A24,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08, VesselPosition.B09, VesselPosition.B10, VesselPosition.B11, VesselPosition.B12, VesselPosition.B13, VesselPosition.B14, VesselPosition.B15, VesselPosition.B16, VesselPosition.B17, VesselPosition.B18, VesselPosition.B19, VesselPosition.B20, VesselPosition.B21, VesselPosition.B22, VesselPosition.B23, VesselPosition.B24,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08, VesselPosition.C09, VesselPosition.C10, VesselPosition.C11, VesselPosition.C12, VesselPosition.C13, VesselPosition.C14, VesselPosition.C15, VesselPosition.C16, VesselPosition.C17, VesselPosition.C18, VesselPosition.C19, VesselPosition.C20, VesselPosition.C21, VesselPosition.C22, VesselPosition.C23, VesselPosition.C24,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08, VesselPosition.D09, VesselPosition.D10, VesselPosition.D11, VesselPosition.D12, VesselPosition.D13, VesselPosition.D14, VesselPosition.D15, VesselPosition.D16, VesselPosition.D17, VesselPosition.D18, VesselPosition.D19, VesselPosition.D20, VesselPosition.D21, VesselPosition.D22, VesselPosition.D23, VesselPosition.D24,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04, VesselPosition.E05, VesselPosition.E06, VesselPosition.E07, VesselPosition.E08, VesselPosition.E09, VesselPosition.E10, VesselPosition.E11, VesselPosition.E12, VesselPosition.E13, VesselPosition.E14, VesselPosition.E15, VesselPosition.E16, VesselPosition.E17, VesselPosition.E18, VesselPosition.E19, VesselPosition.E20, VesselPosition.E21, VesselPosition.E22, VesselPosition.E23, VesselPosition.E24,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04, VesselPosition.F05, VesselPosition.F06, VesselPosition.F07, VesselPosition.F08, VesselPosition.F09, VesselPosition.F10, VesselPosition.F11, VesselPosition.F12, VesselPosition.F13, VesselPosition.F14, VesselPosition.F15, VesselPosition.F16, VesselPosition.F17, VesselPosition.F18, VesselPosition.F19, VesselPosition.F20, VesselPosition.F21, VesselPosition.F22, VesselPosition.F23, VesselPosition.F24,
                    VesselPosition.G01, VesselPosition.G02, VesselPosition.G03, VesselPosition.G04, VesselPosition.G05, VesselPosition.G06, VesselPosition.G07, VesselPosition.G08, VesselPosition.G09, VesselPosition.G10, VesselPosition.G11, VesselPosition.G12, VesselPosition.G13, VesselPosition.G14, VesselPosition.G15, VesselPosition.G16, VesselPosition.G17, VesselPosition.G18, VesselPosition.G19, VesselPosition.G20, VesselPosition.G21, VesselPosition.G22, VesselPosition.G23, VesselPosition.G24,
                    VesselPosition.H01, VesselPosition.H02, VesselPosition.H03, VesselPosition.H04, VesselPosition.H05, VesselPosition.H06, VesselPosition.H07, VesselPosition.H08, VesselPosition.H09, VesselPosition.H10, VesselPosition.H11, VesselPosition.H12, VesselPosition.H13, VesselPosition.H14, VesselPosition.H15, VesselPosition.H16, VesselPosition.H17, VesselPosition.H18, VesselPosition.H19, VesselPosition.H20, VesselPosition.H21, VesselPosition.H22, VesselPosition.H23, VesselPosition.H24,
                    VesselPosition.I01, VesselPosition.I02, VesselPosition.I03, VesselPosition.I04, VesselPosition.I05, VesselPosition.I06, VesselPosition.I07, VesselPosition.I08, VesselPosition.I09, VesselPosition.I10, VesselPosition.I11, VesselPosition.I12, VesselPosition.I13, VesselPosition.I14, VesselPosition.I15, VesselPosition.I16, VesselPosition.I17, VesselPosition.I18, VesselPosition.I19, VesselPosition.I20, VesselPosition.I21, VesselPosition.I22, VesselPosition.I23, VesselPosition.I24,
                    VesselPosition.J01, VesselPosition.J02, VesselPosition.J03, VesselPosition.J04, VesselPosition.J05, VesselPosition.J06, VesselPosition.J07, VesselPosition.J08, VesselPosition.J09, VesselPosition.J10, VesselPosition.J11, VesselPosition.J12, VesselPosition.J13, VesselPosition.J14, VesselPosition.J15, VesselPosition.J16, VesselPosition.J17, VesselPosition.J18, VesselPosition.J19, VesselPosition.J20, VesselPosition.J21, VesselPosition.J22, VesselPosition.J23, VesselPosition.J24,
                    VesselPosition.K01, VesselPosition.K02, VesselPosition.K03, VesselPosition.K04, VesselPosition.K05, VesselPosition.K06, VesselPosition.K07, VesselPosition.K08, VesselPosition.K09, VesselPosition.K10, VesselPosition.K11, VesselPosition.K12, VesselPosition.K13, VesselPosition.K14, VesselPosition.K15, VesselPosition.K16, VesselPosition.K17, VesselPosition.K18, VesselPosition.K19, VesselPosition.K20, VesselPosition.K21, VesselPosition.K22, VesselPosition.K23, VesselPosition.K24,
                    VesselPosition.L01, VesselPosition.L02, VesselPosition.L03, VesselPosition.L04, VesselPosition.L05, VesselPosition.L06, VesselPosition.L07, VesselPosition.L08, VesselPosition.L09, VesselPosition.L10, VesselPosition.L11, VesselPosition.L12, VesselPosition.L13, VesselPosition.L14, VesselPosition.L15, VesselPosition.L16, VesselPosition.L17, VesselPosition.L18, VesselPosition.L19, VesselPosition.L20, VesselPosition.L21, VesselPosition.L22, VesselPosition.L23, VesselPosition.L24,
                    VesselPosition.M01, VesselPosition.M02, VesselPosition.M03, VesselPosition.M04, VesselPosition.M05, VesselPosition.M06, VesselPosition.M07, VesselPosition.M08, VesselPosition.M09, VesselPosition.M10, VesselPosition.M11, VesselPosition.M12, VesselPosition.M13, VesselPosition.M14, VesselPosition.M15, VesselPosition.M16, VesselPosition.M17, VesselPosition.M18, VesselPosition.M19, VesselPosition.M20, VesselPosition.M21, VesselPosition.M22, VesselPosition.M23, VesselPosition.M24,
                    VesselPosition.N01, VesselPosition.N02, VesselPosition.N03, VesselPosition.N04, VesselPosition.N05, VesselPosition.N06, VesselPosition.N07, VesselPosition.N08, VesselPosition.N09, VesselPosition.N10, VesselPosition.N11, VesselPosition.N12, VesselPosition.N13, VesselPosition.N14, VesselPosition.N15, VesselPosition.N16, VesselPosition.N17, VesselPosition.N18, VesselPosition.N19, VesselPosition.N20, VesselPosition.N21, VesselPosition.N22, VesselPosition.N23, VesselPosition.N24,
                    VesselPosition.O01, VesselPosition.O02, VesselPosition.O03, VesselPosition.O04, VesselPosition.O05, VesselPosition.O06, VesselPosition.O07, VesselPosition.O08, VesselPosition.O09, VesselPosition.O10, VesselPosition.O11, VesselPosition.O12, VesselPosition.O13, VesselPosition.O14, VesselPosition.O15, VesselPosition.O16, VesselPosition.O17, VesselPosition.O18, VesselPosition.O19, VesselPosition.O20, VesselPosition.O21, VesselPosition.O22, VesselPosition.O23, VesselPosition.O24,
                    VesselPosition.P01, VesselPosition.P02, VesselPosition.P03, VesselPosition.P04, VesselPosition.P05, VesselPosition.P06, VesselPosition.P07, VesselPosition.P08, VesselPosition.P09, VesselPosition.P10, VesselPosition.P11, VesselPosition.P12, VesselPosition.P13, VesselPosition.P14, VesselPosition.P15, VesselPosition.P16, VesselPosition.P17, VesselPosition.P18, VesselPosition.P19, VesselPosition.P20, VesselPosition.P21, VesselPosition.P22, VesselPosition.P23, VesselPosition.P24,
            }),

    G3x2(
            "3 x 2",
            new String[] {"01", "02", "03"},
            new String[] {"A", "B"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03
            }),

    G4x3(
            "4 x 3",
            new String[] {"01", "02", "03", "04"},
            new String[] {"A", "B", "C"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04
            }),

    G6x4(
            "6 x 4",
            new String[] {"01", "02", "03", "04", "05", "06"},
            new String[] {"A", "B", "C", "D"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06
            }),

    G8x6(
            "8 x 6",
            new String[] {"01", "02", "03", "04", "05", "06", "07", "08"},
            new String[] {"A", "B", "C", "D", "E", "F"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04, VesselPosition.E05, VesselPosition.E06, VesselPosition.E07, VesselPosition.E08,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04, VesselPosition.F05, VesselPosition.F06, VesselPosition.F07, VesselPosition.F08
            }),

    SAGE_CASSETTE(
            "Sage Cassette",
            new String[]{"01"},
            new String[]{"A", "C", "E", "G"},
	    new VesselPosition[]{VesselPosition.A01, VesselPosition.C01, VesselPosition.E01, VesselPosition.G01}),

    FLUIDIGM_48_48(
            "Fluidigm 48.48 Chip",
            new String[]{"04", "05", "06"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"},
            new VesselPosition[]{
                    VesselPosition.A04, VesselPosition.A05, VesselPosition.A06,
                    VesselPosition.B04, VesselPosition.B05, VesselPosition.B06,
                    VesselPosition.C04, VesselPosition.C05, VesselPosition.C06,
                    VesselPosition.D04, VesselPosition.D05, VesselPosition.D06,
                    VesselPosition.E04, VesselPosition.E05, VesselPosition.E06,
                    VesselPosition.F04, VesselPosition.F05, VesselPosition.F06,
                    VesselPosition.G04, VesselPosition.G05, VesselPosition.G06,
                    VesselPosition.H04, VesselPosition.H05, VesselPosition.H06,
                    VesselPosition.I04, VesselPosition.I05, VesselPosition.I06,
                    VesselPosition.J04, VesselPosition.J05, VesselPosition.J06,
                    VesselPosition.K04, VesselPosition.K05, VesselPosition.K06,
                    VesselPosition.L04, VesselPosition.L05, VesselPosition.L06,
                    VesselPosition.M04, VesselPosition.M05, VesselPosition.M06,
                    VesselPosition.N04, VesselPosition.N05, VesselPosition.N06,
                    VesselPosition.O04, VesselPosition.O05, VesselPosition.O06,
                    VesselPosition.P04, VesselPosition.P05, VesselPosition.P06,
            }),
    FLUIDIGM_96_96(
            "Fluidigm 96.96 Chip",
            new String[]{"07", "08", "09", "10", "11", "12"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"},
            new VesselPosition[]{
                    VesselPosition.A07, VesselPosition.A08, VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12,
                    VesselPosition.B07, VesselPosition.B08, VesselPosition.B09, VesselPosition.B10, VesselPosition.B11, VesselPosition.B12,
                    VesselPosition.C07, VesselPosition.C08, VesselPosition.C09, VesselPosition.C10, VesselPosition.C11, VesselPosition.C12,
                    VesselPosition.D07, VesselPosition.D08, VesselPosition.D09, VesselPosition.D10, VesselPosition.D11, VesselPosition.D12,
                    VesselPosition.E07, VesselPosition.E08, VesselPosition.E09, VesselPosition.E10, VesselPosition.E11, VesselPosition.E12,
                    VesselPosition.F07, VesselPosition.F08, VesselPosition.F09, VesselPosition.F10, VesselPosition.F11, VesselPosition.F12,
                    VesselPosition.G07, VesselPosition.G08, VesselPosition.G09, VesselPosition.G10, VesselPosition.G11, VesselPosition.G12,
                    VesselPosition.H07, VesselPosition.H08, VesselPosition.H09, VesselPosition.H10, VesselPosition.H11, VesselPosition.H12,
                    VesselPosition.I07, VesselPosition.I08, VesselPosition.I09, VesselPosition.I10, VesselPosition.I11, VesselPosition.I12,
                    VesselPosition.J07, VesselPosition.J08, VesselPosition.J09, VesselPosition.J10, VesselPosition.J11, VesselPosition.J12,
                    VesselPosition.K07, VesselPosition.K08, VesselPosition.K09, VesselPosition.K10, VesselPosition.K11, VesselPosition.K12,
                    VesselPosition.L07, VesselPosition.L08, VesselPosition.L09, VesselPosition.L10, VesselPosition.L11, VesselPosition.L12,
                    VesselPosition.M07, VesselPosition.M08, VesselPosition.M09, VesselPosition.M10, VesselPosition.M11, VesselPosition.M12,
                    VesselPosition.N07, VesselPosition.N08, VesselPosition.N09, VesselPosition.N10, VesselPosition.N11, VesselPosition.N12,
                    VesselPosition.O07, VesselPosition.O08, VesselPosition.O09, VesselPosition.O10, VesselPosition.O11, VesselPosition.O12,
                    VesselPosition.P07, VesselPosition.P08, VesselPosition.P09, VesselPosition.P10, VesselPosition.P11, VesselPosition.P12
            }),


    STRIP_TUBE(
            "1x8",
            new String[]{""},
            new String[]{"TUBE1", "TUBE2", "TUBE3", "TUBE4", "TUBE5", "TUBE6", "TUBE7", "TUBE8"},
            new VesselPosition[]{VesselPosition.TUBE1, VesselPosition.TUBE2, VesselPosition.TUBE3, VesselPosition.TUBE4, VesselPosition.TUBE5, VesselPosition.TUBE6, VesselPosition.TUBE7, VesselPosition.TUBE8}),

    STRIP_TUBE_WELL(
            "1x1",
            new String[]{""},
            new String[]{""}),

    FLOWCELL1x8(
            "1x8",
            new String[]{""},
            new String[]{"LANE1", "LANE2", "LANE3", "LANE4", "LANE5", "LANE6", "LANE7", "LANE8"},
            new VesselPosition[]{VesselPosition.LANE1, VesselPosition.LANE2, VesselPosition.LANE3, VesselPosition.LANE4, VesselPosition.LANE5, VesselPosition.LANE6, VesselPosition.LANE7, VesselPosition.LANE8}),

    FLOWCELL1x4(
            "1x4",
            new String[]{""},
            new String[]{"LANE1", "LANE2", "LANE3", "LANE4"},
            new VesselPosition[]{VesselPosition.LANE1, VesselPosition.LANE2, VesselPosition.LANE3, VesselPosition.LANE4}),

    FLOWCELL1x2(
            "1x2",
            new String[]{""},
            new String[]{"LANE1", "LANE2"},
            new VesselPosition[]{VesselPosition.LANE1, VesselPosition.LANE2}),

    FLOWCELL1x1(
            "1x1",
            new String[]{""},
            new String[]{"LANE1"},
            new VesselPosition[]{VesselPosition.LANE1}),

    MISEQ_REAGENT_KIT(
            "MiSeq Reagent Kit",
            new String[]{"04"},
            new String[]{"D"},
            new VesselPosition[]{VesselPosition.D04}),


    RUN_CHAMBER(
                "1x1",
                new String[]{""},
                new String[]{""}),

    G32x1(
            "32x1",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32"},
            new String[]{"A"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12, VesselPosition.A13, VesselPosition.A14, VesselPosition.A15, VesselPosition.A16,
                    VesselPosition.A17, VesselPosition.A18, VesselPosition.A19, VesselPosition.A20, VesselPosition.A21, VesselPosition.A22, VesselPosition.A23, VesselPosition.A24,
                    VesselPosition.A25, VesselPosition.A26, VesselPosition.A27, VesselPosition.A28, VesselPosition.A29, VesselPosition.A30, VesselPosition.A31, VesselPosition.A32,
    }),

    G8x1(
            "8x1",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08"},
            new String[]{"A"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08
            }),

    G24x1(
            "24x1",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"},
            new String[]{"A"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12, VesselPosition.A13, VesselPosition.A14, VesselPosition.A15, VesselPosition.A16,
                    VesselPosition.A17, VesselPosition.A18, VesselPosition.A19, VesselPosition.A20, VesselPosition.A21, VesselPosition.A22, VesselPosition.A23, VesselPosition.A24
            }),

    G24x4(
            "24x4",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"},
            new String[]{"A", "B", "C", "D"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12, VesselPosition.A13, VesselPosition.A14, VesselPosition.A15, VesselPosition.A16,
                    VesselPosition.A17, VesselPosition.A18, VesselPosition.A19, VesselPosition.A20, VesselPosition.A21, VesselPosition.A22, VesselPosition.A23, VesselPosition.A24,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08,
                    VesselPosition.B09, VesselPosition.B10, VesselPosition.B11, VesselPosition.B12, VesselPosition.B13, VesselPosition.B14, VesselPosition.B15, VesselPosition.B16,
                    VesselPosition.B17, VesselPosition.B18, VesselPosition.B19, VesselPosition.B20, VesselPosition.B21, VesselPosition.B22, VesselPosition.B23, VesselPosition.B24,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08,
                    VesselPosition.C09, VesselPosition.C10, VesselPosition.C11, VesselPosition.C12, VesselPosition.C13, VesselPosition.C14, VesselPosition.C15, VesselPosition.C16,
                    VesselPosition.C17, VesselPosition.C18, VesselPosition.C19, VesselPosition.C20, VesselPosition.C21, VesselPosition.C22, VesselPosition.C23, VesselPosition.C24,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08,
                    VesselPosition.D09, VesselPosition.D10, VesselPosition.D11, VesselPosition.D12, VesselPosition.D13, VesselPosition.D14, VesselPosition.D15, VesselPosition.D16,
                    VesselPosition.D17, VesselPosition.D18, VesselPosition.D19, VesselPosition.D20, VesselPosition.D21, VesselPosition.D22, VesselPosition.D23, VesselPosition.D24
            }),
    G4x24(
            "4x24",
            new String[]{"01", "02", "03", "04"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04,
                    VesselPosition.G01, VesselPosition.G02, VesselPosition.G03, VesselPosition.G04,
                    VesselPosition.H01, VesselPosition.H02, VesselPosition.H03, VesselPosition.H04,
                    VesselPosition.I01, VesselPosition.I02, VesselPosition.I03, VesselPosition.I04,
                    VesselPosition.J01, VesselPosition.J02, VesselPosition.J03, VesselPosition.J04,
                    VesselPosition.K01, VesselPosition.K02, VesselPosition.K03, VesselPosition.K04,
                    VesselPosition.L01, VesselPosition.L02, VesselPosition.L03, VesselPosition.L04,
                    VesselPosition.M01, VesselPosition.M02, VesselPosition.M03, VesselPosition.M04,
                    VesselPosition.N01, VesselPosition.N02, VesselPosition.N03, VesselPosition.N04,
                    VesselPosition.O01, VesselPosition.O02, VesselPosition.O03, VesselPosition.O04,
                    VesselPosition.P01, VesselPosition.P02, VesselPosition.P03, VesselPosition.P04,
                    VesselPosition.Q01, VesselPosition.Q02, VesselPosition.Q03, VesselPosition.Q04,
                    VesselPosition.R01, VesselPosition.R02, VesselPosition.R03, VesselPosition.R04,
                    VesselPosition.S01, VesselPosition.S02, VesselPosition.S03, VesselPosition.S04,
                    VesselPosition.T01, VesselPosition.T02, VesselPosition.T03, VesselPosition.T04,
                    VesselPosition.U01, VesselPosition.U02, VesselPosition.U03, VesselPosition.U04,
                    VesselPosition.V01, VesselPosition.V02, VesselPosition.V03, VesselPosition.V04,
                    VesselPosition.W01, VesselPosition.W02, VesselPosition.W03, VesselPosition.W04,
                    VesselPosition.X01, VesselPosition.X02, VesselPosition.X03, VesselPosition.X04
            }),

    G1x100_NUM("1 x 100",
            new String[]{"01"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32",
                    "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48",
                    "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64",
                    "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80",
                    "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96",
                    "97", "98", "99", "100"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._2_1, VesselPosition._3_1, VesselPosition._4_1,
                    VesselPosition._5_1, VesselPosition._6_1, VesselPosition._7_1, VesselPosition._8_1,
                    VesselPosition._9_1, VesselPosition._10_1, VesselPosition._11_1, VesselPosition._12_1,
                    VesselPosition._13_1, VesselPosition._14_1, VesselPosition._15_1, VesselPosition._16_1,
                    VesselPosition._17_1, VesselPosition._18_1, VesselPosition._19_1, VesselPosition._20_1,
                    VesselPosition._21_1, VesselPosition._22_1, VesselPosition._23_1, VesselPosition._24_1,
                    VesselPosition._25_1, VesselPosition._26_1, VesselPosition._27_1, VesselPosition._28_1,
                    VesselPosition._29_1, VesselPosition._30_1, VesselPosition._31_1, VesselPosition._32_1,
                    VesselPosition._33_1, VesselPosition._34_1, VesselPosition._35_1, VesselPosition._36_1,
                    VesselPosition._37_1, VesselPosition._38_1, VesselPosition._39_1, VesselPosition._40_1,
                    VesselPosition._41_1, VesselPosition._42_1, VesselPosition._43_1, VesselPosition._44_1,
                    VesselPosition._45_1, VesselPosition._46_1, VesselPosition._47_1, VesselPosition._48_1,
                    VesselPosition._49_1, VesselPosition._50_1, VesselPosition._51_1, VesselPosition._52_1,
                    VesselPosition._53_1, VesselPosition._54_1, VesselPosition._55_1, VesselPosition._56_1,
                    VesselPosition._57_1, VesselPosition._58_1, VesselPosition._59_1, VesselPosition._60_1,
                    VesselPosition._61_1, VesselPosition._62_1, VesselPosition._63_1, VesselPosition._64_1,
                    VesselPosition._65_1, VesselPosition._66_1, VesselPosition._67_1, VesselPosition._68_1,
                    VesselPosition._69_1, VesselPosition._70_1, VesselPosition._71_1, VesselPosition._72_1,
                    VesselPosition._73_1, VesselPosition._74_1, VesselPosition._75_1, VesselPosition._76_1,
                    VesselPosition._77_1, VesselPosition._78_1, VesselPosition._79_1, VesselPosition._80_1,
                    VesselPosition._81_1, VesselPosition._82_1, VesselPosition._83_1, VesselPosition._84_1,
                    VesselPosition._85_1, VesselPosition._86_1, VesselPosition._87_1, VesselPosition._88_1,
                    VesselPosition._89_1, VesselPosition._90_1, VesselPosition._91_1, VesselPosition._92_1,
                    VesselPosition._93_1, VesselPosition._94_1, VesselPosition._95_1, VesselPosition._96_1,
                    VesselPosition._97_1, VesselPosition._98_1, VesselPosition._99_1, VesselPosition._100_1}),

    G1x10_NUM("1 x 10",
            new String[]{"01"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._2_1, VesselPosition._3_1, VesselPosition._4_1,
                    VesselPosition._5_1, VesselPosition._6_1, VesselPosition._7_1, VesselPosition._8_1,
                    VesselPosition._9_1, VesselPosition._10_1}),

    G1x25_NUM("1 x 25",
            new String[]{"01"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "21", "22", "23", "24", "25"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._2_1, VesselPosition._3_1, VesselPosition._4_1,
                    VesselPosition._5_1, VesselPosition._6_1, VesselPosition._7_1, VesselPosition._8_1,
                    VesselPosition._9_1, VesselPosition._10_1, VesselPosition._11_1, VesselPosition._12_1,
                    VesselPosition._13_1, VesselPosition._14_1, VesselPosition._15_1, VesselPosition._16_1,
                    VesselPosition._17_1, VesselPosition._18_1, VesselPosition._19_1, VesselPosition._20_1,
                    VesselPosition._21_1, VesselPosition._22_1, VesselPosition._23_1, VesselPosition._24_1,
                    VesselPosition._25_1}),

    G2x25_NUM("2 x 25",
            new String[]{"01", "02"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "21", "22", "23", "24", "25"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._2_1, VesselPosition._2_2,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._4_1, VesselPosition._4_2,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._6_1, VesselPosition._6_2,
                    VesselPosition._7_1, VesselPosition._7_2, VesselPosition._8_1, VesselPosition._8_2,
                    VesselPosition._9_1, VesselPosition._9_2, VesselPosition._10_1, VesselPosition._10_2,
                    VesselPosition._11_1, VesselPosition._11_2, VesselPosition._12_1, VesselPosition._12_2,
                    VesselPosition._13_1, VesselPosition._13_2, VesselPosition._14_1, VesselPosition._14_2,
                    VesselPosition._15_1, VesselPosition._15_2, VesselPosition._16_1, VesselPosition._16_2,
                    VesselPosition._17_1, VesselPosition._17_2, VesselPosition._18_1, VesselPosition._18_2,
                    VesselPosition._19_1, VesselPosition._19_2, VesselPosition._20_1, VesselPosition._20_2,
                    VesselPosition._21_1, VesselPosition._21_2, VesselPosition._22_1, VesselPosition._22_2,
                    VesselPosition._23_1, VesselPosition._23_2, VesselPosition._24_1, VesselPosition._24_2,
                    VesselPosition._25_1, VesselPosition._25_2}),

    G2x50_NUM("2 x 50",
            new String[]{"01", "02"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32",
                    "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48",
                    "49", "50"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._2_1, VesselPosition._2_2,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._4_1, VesselPosition._4_2,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._6_1, VesselPosition._6_2,
                    VesselPosition._7_1, VesselPosition._7_2, VesselPosition._8_1, VesselPosition._8_2,
                    VesselPosition._9_1, VesselPosition._9_2, VesselPosition._10_1, VesselPosition._10_2,
                    VesselPosition._11_1, VesselPosition._11_2, VesselPosition._12_1, VesselPosition._12_2,
                    VesselPosition._13_1, VesselPosition._13_2, VesselPosition._14_1, VesselPosition._14_2,
                    VesselPosition._15_1, VesselPosition._15_2, VesselPosition._16_1, VesselPosition._16_2,
                    VesselPosition._17_1, VesselPosition._17_2, VesselPosition._18_1, VesselPosition._18_2,
                    VesselPosition._19_1, VesselPosition._19_2, VesselPosition._20_1, VesselPosition._20_2,
                    VesselPosition._21_1, VesselPosition._21_2, VesselPosition._22_1, VesselPosition._22_2,
                    VesselPosition._23_1, VesselPosition._23_2, VesselPosition._24_1, VesselPosition._24_2,
                    VesselPosition._25_1, VesselPosition._25_2, VesselPosition._26_1, VesselPosition._26_2,
                    VesselPosition._27_1, VesselPosition._27_2, VesselPosition._28_1, VesselPosition._28_2,
                    VesselPosition._29_1, VesselPosition._29_2, VesselPosition._30_1, VesselPosition._30_2,
                    VesselPosition._31_1, VesselPosition._31_2, VesselPosition._32_1, VesselPosition._32_2,
                    VesselPosition._33_1, VesselPosition._33_2, VesselPosition._34_1, VesselPosition._34_2,
                    VesselPosition._35_1, VesselPosition._35_2, VesselPosition._36_1, VesselPosition._36_2,
                    VesselPosition._37_1, VesselPosition._37_2, VesselPosition._38_1, VesselPosition._38_2,
                    VesselPosition._39_1, VesselPosition._39_2, VesselPosition._40_1, VesselPosition._40_2,
                    VesselPosition._41_1, VesselPosition._41_2, VesselPosition._42_1, VesselPosition._42_2,
                    VesselPosition._43_1, VesselPosition._43_2, VesselPosition._44_1, VesselPosition._44_2,
                    VesselPosition._45_1, VesselPosition._45_2, VesselPosition._46_1, VesselPosition._46_2,
                    VesselPosition._47_1, VesselPosition._47_2, VesselPosition._48_1, VesselPosition._48_2,
                    VesselPosition._49_1, VesselPosition._49_2, VesselPosition._50_1, VesselPosition._50_2}),

    G3x7_NUM("3 x 7",
            new String[]{"01", "02", "03"},
            new String[]{"01", "02", "03", "04", "05", "06", "07"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._2_1,
                    VesselPosition._2_2, VesselPosition._2_3, VesselPosition._3_1, VesselPosition._3_2,
                    VesselPosition._3_3, VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._6_1,
                    VesselPosition._6_2, VesselPosition._6_3, VesselPosition._7_1, VesselPosition._7_2,
                    VesselPosition._7_3}),

    G3x8_NUM("3 x 8",
            new String[]{"01", "02", "03"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._2_1,
                    VesselPosition._2_2, VesselPosition._2_3, VesselPosition._3_1, VesselPosition._3_2,
                    VesselPosition._3_3, VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._6_1,
                    VesselPosition._6_2, VesselPosition._6_3, VesselPosition._7_1, VesselPosition._7_2,
                    VesselPosition._7_3, VesselPosition._8_1, VesselPosition._8_2, VesselPosition._8_3}),

    G4x3_NUM("4 x 3",
            new String[]{"01", "02", "03", "04"},
            new String[]{"01", "02", "03"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3, VesselPosition._2_4,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4}),

    G4x4_NUM("4 x 4",
            new String[]{"01", "02", "03", "04"},
            new String[]{"01", "02", "03", "04"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3, VesselPosition._2_4,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4,
                    VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3, VesselPosition._4_4}),

    G4x6_NUM("4 x 6",
            new String[]{"01", "02", "03", "04"},
            new String[]{"01", "02", "03", "04", "05", "06"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3, VesselPosition._2_4,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4,
                    VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3, VesselPosition._4_4,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4,
                    VesselPosition._6_1, VesselPosition._6_2, VesselPosition._6_3, VesselPosition._6_4}),

    G4x10_NUM("4 x 10",
            new String[]{"01", "02", "03", "04"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3, VesselPosition._2_4,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4,
                    VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3, VesselPosition._4_4,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4,
                    VesselPosition._6_1, VesselPosition._6_2, VesselPosition._6_3, VesselPosition._6_4,
                    VesselPosition._7_1, VesselPosition._7_2, VesselPosition._7_3, VesselPosition._7_4,
                    VesselPosition._8_1, VesselPosition._8_2, VesselPosition._8_3, VesselPosition._8_4,
                    VesselPosition._9_1, VesselPosition._9_2, VesselPosition._9_3, VesselPosition._9_4,
                    VesselPosition._10_1, VesselPosition._10_2, VesselPosition._10_3, VesselPosition._10_4}),

    G5x3_NUM("5 x 3",
            new String[]{"01", "02", "03", "04", "05"},
            new String[]{"01", "02", "03"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._1_5, VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3,
                    VesselPosition._2_4, VesselPosition._2_5, VesselPosition._3_1, VesselPosition._3_2,
                    VesselPosition._3_3, VesselPosition._3_4, VesselPosition._3_5}),

    G6x4_ALPHANUM("6 x 4",
            new String[]{"01", "02", "03", "04", "05", "06"},
            new String[]{"A", "B", "C", "D"},
            new VesselPosition[]{VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06}),

    G6x6_NUM("6 x 6",
            new String[]{"01", "02", "03", "04", "05", "06"},
            new String[]{"01", "02", "03", "04", "05", "06"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._1_5, VesselPosition._1_6, VesselPosition._2_1, VesselPosition._2_2,
                    VesselPosition._2_3, VesselPosition._2_4, VesselPosition._2_5, VesselPosition._2_6,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4,
                    VesselPosition._3_5, VesselPosition._3_6, VesselPosition._4_1, VesselPosition._4_2,
                    VesselPosition._4_3, VesselPosition._4_4, VesselPosition._4_5, VesselPosition._4_6,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4,
                    VesselPosition._5_5, VesselPosition._5_6, VesselPosition._6_1, VesselPosition._6_2,
                    VesselPosition._6_3, VesselPosition._6_4, VesselPosition._6_5, VesselPosition._6_6}),

    G7x7_NUM("7 x 7",
            new String[]{"01", "02", "03", "04", "05", "06", "07"},
            new String[]{"01", "02", "03", "04", "05", "06", "07"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._1_5, VesselPosition._1_6, VesselPosition._1_7, VesselPosition._2_1,
                    VesselPosition._2_2, VesselPosition._2_3, VesselPosition._2_4, VesselPosition._2_5,
                    VesselPosition._2_6, VesselPosition._2_7, VesselPosition._3_1, VesselPosition._3_2,
                    VesselPosition._3_3, VesselPosition._3_4, VesselPosition._3_5, VesselPosition._3_6,
                    VesselPosition._3_7, VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3,
                    VesselPosition._4_4, VesselPosition._4_5, VesselPosition._4_6, VesselPosition._4_7,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4,
                    VesselPosition._5_5, VesselPosition._5_6, VesselPosition._5_7, VesselPosition._6_1,
                    VesselPosition._6_2, VesselPosition._6_3, VesselPosition._6_4, VesselPosition._6_5,
                    VesselPosition._6_6, VesselPosition._6_7, VesselPosition._7_1, VesselPosition._7_2,
                    VesselPosition._7_3, VesselPosition._7_4, VesselPosition._7_5, VesselPosition._7_6,
                    VesselPosition._7_7}),

    G9x9_NUM("9 x 9",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._1_5, VesselPosition._1_6, VesselPosition._1_7, VesselPosition._1_8,
                    VesselPosition._1_9, VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3,
                    VesselPosition._2_4, VesselPosition._2_5, VesselPosition._2_6, VesselPosition._2_7,
                    VesselPosition._2_8, VesselPosition._2_9, VesselPosition._3_1, VesselPosition._3_2,
                    VesselPosition._3_3, VesselPosition._3_4, VesselPosition._3_5, VesselPosition._3_6,
                    VesselPosition._3_7, VesselPosition._3_8, VesselPosition._3_9, VesselPosition._4_1,
                    VesselPosition._4_2, VesselPosition._4_3, VesselPosition._4_4, VesselPosition._4_5,
                    VesselPosition._4_6, VesselPosition._4_7, VesselPosition._4_8, VesselPosition._4_9,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4,
                    VesselPosition._5_5, VesselPosition._5_6, VesselPosition._5_7, VesselPosition._5_8,
                    VesselPosition._5_9, VesselPosition._6_1, VesselPosition._6_2, VesselPosition._6_3,
                    VesselPosition._6_4, VesselPosition._6_5, VesselPosition._6_6, VesselPosition._6_7,
                    VesselPosition._6_8, VesselPosition._6_9, VesselPosition._7_1, VesselPosition._7_2,
                    VesselPosition._7_3, VesselPosition._7_4, VesselPosition._7_5, VesselPosition._7_6,
                    VesselPosition._7_7, VesselPosition._7_8, VesselPosition._7_9, VesselPosition._8_1,
                    VesselPosition._8_2, VesselPosition._8_3, VesselPosition._8_4, VesselPosition._8_5,
                    VesselPosition._8_6, VesselPosition._8_7, VesselPosition._8_8, VesselPosition._8_9,
                    VesselPosition._9_1, VesselPosition._9_2, VesselPosition._9_3, VesselPosition._9_4,
                    VesselPosition._9_5, VesselPosition._9_6, VesselPosition._9_7, VesselPosition._9_8,
                    VesselPosition._9_9}),

    G10x10_NUM("10 x 10",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._1_5, VesselPosition._1_6, VesselPosition._1_7, VesselPosition._1_8,
                    VesselPosition._1_9, VesselPosition._1_10, VesselPosition._2_1, VesselPosition._2_2,
                    VesselPosition._2_3, VesselPosition._2_4, VesselPosition._2_5, VesselPosition._2_6,
                    VesselPosition._2_7, VesselPosition._2_8, VesselPosition._2_9, VesselPosition._2_10,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4,
                    VesselPosition._3_5, VesselPosition._3_6, VesselPosition._3_7, VesselPosition._3_8,
                    VesselPosition._3_9, VesselPosition._3_10, VesselPosition._4_1, VesselPosition._4_2,
                    VesselPosition._4_3, VesselPosition._4_4, VesselPosition._4_5, VesselPosition._4_6,
                    VesselPosition._4_7, VesselPosition._4_8, VesselPosition._4_9, VesselPosition._4_10,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4,
                    VesselPosition._5_5, VesselPosition._5_6, VesselPosition._5_7, VesselPosition._5_8,
                    VesselPosition._5_9, VesselPosition._5_10, VesselPosition._6_1, VesselPosition._6_2,
                    VesselPosition._6_3, VesselPosition._6_4, VesselPosition._6_5, VesselPosition._6_6,
                    VesselPosition._6_7, VesselPosition._6_8, VesselPosition._6_9, VesselPosition._6_10,
                    VesselPosition._7_1, VesselPosition._7_2, VesselPosition._7_3, VesselPosition._7_4,
                    VesselPosition._7_5, VesselPosition._7_6, VesselPosition._7_7, VesselPosition._7_8,
                    VesselPosition._7_9, VesselPosition._7_10, VesselPosition._8_1, VesselPosition._8_2,
                    VesselPosition._8_3, VesselPosition._8_4, VesselPosition._8_5, VesselPosition._8_6,
                    VesselPosition._8_7, VesselPosition._8_8, VesselPosition._8_9, VesselPosition._8_10,
                    VesselPosition._9_1, VesselPosition._9_2, VesselPosition._9_3, VesselPosition._9_4,
                    VesselPosition._9_5, VesselPosition._9_6, VesselPosition._9_7, VesselPosition._9_8,
                    VesselPosition._9_9, VesselPosition._9_10, VesselPosition._10_1, VesselPosition._10_2,
                    VesselPosition._10_3, VesselPosition._10_4, VesselPosition._10_5, VesselPosition._10_6,
                    VesselPosition._10_7, VesselPosition._10_8, VesselPosition._10_9, VesselPosition._10_10}),

    G10x1_NUM("10 x 1",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"},
            new String[]{"01"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4,
                    VesselPosition._1_5, VesselPosition._1_6, VesselPosition._1_7, VesselPosition._1_8,
                    VesselPosition._1_9, VesselPosition._1_10}),

    G6x12_NUM("6 x 12",
            new String[]{"01", "02", "03", "04", "05", "06"},
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"},
            new VesselPosition[]{VesselPosition._1_1, VesselPosition._1_2, VesselPosition._1_3, VesselPosition._1_4, VesselPosition._1_5, VesselPosition._1_6,
                    VesselPosition._2_1, VesselPosition._2_2, VesselPosition._2_3, VesselPosition._2_4, VesselPosition._2_5, VesselPosition._2_6,
                    VesselPosition._3_1, VesselPosition._3_2, VesselPosition._3_3, VesselPosition._3_4, VesselPosition._3_5, VesselPosition._3_6,
                    VesselPosition._4_1, VesselPosition._4_2, VesselPosition._4_3, VesselPosition._4_4, VesselPosition._4_5, VesselPosition._4_6,
                    VesselPosition._5_1, VesselPosition._5_2, VesselPosition._5_3, VesselPosition._5_4, VesselPosition._5_5, VesselPosition._5_6,
                    VesselPosition._6_1, VesselPosition._6_2, VesselPosition._6_3, VesselPosition._6_4, VesselPosition._6_5, VesselPosition._6_6,
                    VesselPosition._7_1, VesselPosition._7_2, VesselPosition._7_3, VesselPosition._7_4, VesselPosition._7_5, VesselPosition._7_6,
                    VesselPosition._8_1, VesselPosition._8_2, VesselPosition._8_3, VesselPosition._8_4, VesselPosition._8_5, VesselPosition._8_6,
                    VesselPosition._9_1, VesselPosition._9_2, VesselPosition._9_3, VesselPosition._9_4, VesselPosition._9_5, VesselPosition._9_6,
                    VesselPosition._10_1, VesselPosition._10_2, VesselPosition._10_3, VesselPosition._10_4, VesselPosition._10_5, VesselPosition._10_6,
                    VesselPosition._11_1, VesselPosition._11_2, VesselPosition._11_3, VesselPosition._11_4, VesselPosition._11_5, VesselPosition._11_6,
                    VesselPosition._12_1, VesselPosition._12_2, VesselPosition._12_3, VesselPosition._12_4, VesselPosition._12_5, VesselPosition._12_6}),

    INFINIUM_24_CHIP("2 x 12",
            new String[] {"C01","C02"},
            new String[] {"R01","R02","R03","R04","R05","R06","R07","R08","R09","R10","R11","R12"},
            new VesselPosition[]{VesselPosition.R01C01, VesselPosition.R01C02,
                    VesselPosition.R02C01, VesselPosition.R02C02,
                    VesselPosition.R03C01, VesselPosition.R03C02,
                    VesselPosition.R04C01, VesselPosition.R04C02,
                    VesselPosition.R05C01, VesselPosition.R05C02,
                    VesselPosition.R06C01, VesselPosition.R06C02,
                    VesselPosition.R07C01, VesselPosition.R07C02,
                    VesselPosition.R08C01, VesselPosition.R08C02,
                    VesselPosition.R09C01, VesselPosition.R09C02,
                    VesselPosition.R10C01, VesselPosition.R10C02,
                    VesselPosition.R11C01, VesselPosition.R11C02,
                    VesselPosition.R12C01, VesselPosition.R12C02}),

    INFINIUM_12_CHIP("2 x 6",
            new String[] {"C01","C02"},
            new String[] {"R01","R02","R03","R04","R05","R06"},
            new VesselPosition[]{VesselPosition.R01C01, VesselPosition.R01C02,
                    VesselPosition.R02C01, VesselPosition.R02C02,
                    VesselPosition.R03C01, VesselPosition.R03C02,
                    VesselPosition.R04C01, VesselPosition.R04C02,
                    VesselPosition.R05C01, VesselPosition.R05C02,
                    VesselPosition.R06C01, VesselPosition.R06C02}),

    INFINIUM_8_CHIP("1 x 8",
            new String[] {"C01"},
            new String[] {"R01","R02","R03","R04","R05","R06","R07","R08"},
            new VesselPosition[]{VesselPosition.R01C01, VesselPosition.R02C01,VesselPosition.R03C01,
                    VesselPosition.R04C01, VesselPosition.R05C01, VesselPosition.R06C01,
                    VesselPosition.R07C01,VesselPosition.R08C01}),

    TEN_X_CHIP("1 x 8",
            new String[] {"01"},
            new String[] {"A","B","C","D","E","F","G","H"},
            new VesselPosition[]{VesselPosition.A01, VesselPosition.B01,VesselPosition.C01,
                    VesselPosition.D01, VesselPosition.E01, VesselPosition.F01,
                    VesselPosition.G01,VesselPosition.H01}),

    // todo jmt change TubeFormation to VesselFormation so we can store formations of PlateType.InfiniumChip
    TEFLOW3x8("TeFlow 3 x 8",
            new String[]{"", "", ""},
            new String[]{"", "", "", "", "", "", "", ""},
            new VesselPosition[]{VesselPosition._1, VesselPosition._9, VesselPosition._17,
                    VesselPosition._2, VesselPosition._10, VesselPosition._18,
                    VesselPosition._3, VesselPosition._11, VesselPosition._19,
                    VesselPosition._4, VesselPosition._12, VesselPosition._20,
                    VesselPosition._5, VesselPosition._13, VesselPosition._21,
                    VesselPosition._6, VesselPosition._14, VesselPosition._22,
                    VesselPosition._7, VesselPosition._15, VesselPosition._23,
                    VesselPosition._8, VesselPosition._16, VesselPosition._24,
            });



    private final String name;
    private final String[] columnNames;
    private final String[] rowNames;
    private final Integer capacity;
    private VesselPosition[] vesselPositions;
    private Map<VesselPosition, RowColumn> mapVesselPositionToRowColumn = new HashMap<>();

    public class RowColumn implements Serializable {
        private int row;
        private int column;

        public RowColumn(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RowColumn rowColumn = (RowColumn) o;
            return row == rowColumn.row &&
                    column == rowColumn.column;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, column);
        }
    }

    VesselGeometry(String name, String[] columnNames, String[] rowNames) {
        this.name = name;
        this.columnNames = columnNames;
        this.rowNames = rowNames;
        this.capacity = columnNames.length * rowNames.length;
    }

    VesselGeometry(String name, String[] columnNames, String[] rowNames, VesselPosition[] vesselPositions) {
        this(name, columnNames, rowNames);
        this.vesselPositions = vesselPositions;
        int columnIndex = 0;
        int rowIndex= 0;

        for (VesselPosition vesselPosition : vesselPositions) {
            // RowColumn uses 1-based numbering.
            mapVesselPositionToRowColumn.put(vesselPosition, new RowColumn(rowIndex + 1, columnIndex + 1));
            columnIndex++;
            if(columnIndex >= columnNames.length) {
                columnIndex = 0;
                rowIndex++;
            }
        }
    }

    public String getName() {
        return name;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return rowNames.length;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public Integer getCapacity(){
        return capacity;
    }

    public Iterator<String> getPositionNames() {
        return new Iterator<String>() {
            private int columnIndex = 0;
            private int rowIndex= 0;

            @Override
            public boolean hasNext() {
                return columnIndex < columnNames.length && rowIndex < rowNames.length;
            }

            @Override
            public String next() {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                String next = rowNames[rowIndex] + columnNames[columnIndex];
                columnIndex++;
                if(columnIndex >= columnNames.length) {
                    columnIndex = 0;
                    rowIndex++;
                }
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    public VesselPosition[] getVesselPositions() {
        return vesselPositions;
    }

    public RowColumn getRowColumnForVesselPosition(VesselPosition vesselPosition) {
        return mapVesselPositionToRowColumn.get(vesselPosition);
    }

    /** Returns a RowColumn. RowColumn uses 1-based numbering. */
    public RowColumn makeRowColumn(int row, int column) {
        return new RowColumn(row, column);
    }
}
