package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Stripes action bean to allow upload of files describing plates of molecular indexes.
 */
@UrlBinding("/reagent/molindplate.action")
public class MolecularIndexPlateActionBean extends CoreActionBean {
    public static final String MOL_IND_PLATE_PAGE = "/reagent/mol_ind_plate_upload.jsp";
    public static final String UPLOAD_ACTION = "upload";

    public static final String PLATE_DEFINITION_PAGE = "/reagent/index_plate_def.jsp";
    public static final String MAKE_DEFINITION = "makeDefinition";
    public static final String UPLOAD_DEFINITION = "uploadDefinition";

    public static final String PLATE_INSTANCE_PAGE = "/reagent/index_plate_instance.jsp";
    public static final String MAKE_INSTANCE = "makeInstance";
    public static final String FIND_LAYOUT = "findLayout";
    public static final String CREATE_INSTANCE = "createInstance";

    public static final String PLATE_DEFINITION_MANAGEMENT_PAGE = "/reagent/index_plate_def_manage.jsp";
    public static final String MANAGE_DEFINITION = "manageDefinition";
    public static final String RENAME_DEFINITION = "renameDefinition";
    public static final String DELETE_DEFINITION = "deleteDefinition";
    public static final String FIND_INSTANCES = "findInstances";

    public static final String PLATE_INSTANCE_MANAGEMENT_PAGE = "/reagent/index_plate_inst_manage.jsp";
    public static final String MANAGE_INSTANCES = "manageInstances";
    public static final String DELETE_INSTANCES = "deleteInstances";
    public static final String FIND_DEFINITION = "findDefinition";

    @Validate(required = true, on = {UPLOAD_DEFINITION})
    private String plateGeometry;

    @Validate(required = true, on = {UPLOAD_DEFINITION})
    private String reagentType;

    @Validate(required = true, on = {CREATE_INSTANCE})
    private String salesOrderNumber;

    private boolean allowOverwrite;

    @Validate(required = true, on = {UPLOAD_DEFINITION, FIND_LAYOUT, CREATE_INSTANCE})
    private String plateName;

    @Validate(required = true, on = {UPLOAD_DEFINITION, CREATE_INSTANCE})
    private FileBean spreadsheet;

    private MessageCollection messageCollection = new MessageCollection();
    private List<String> plateNames = new ArrayList<>();
    private List<List<String>> plateLayout = new ArrayList<>();
    private List<String> plateBarcodes = new ArrayList<>();

    @Validate(required = true, on = {RENAME_DEFINITION})
    private String newDefinitionName;

    private IndexedPlateFactory.TechnologiesAndParsers technologyAndParserType;

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        if (spreadsheet != null) {
            try {
                Map<String, StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseAndPersist(
                        technologyAndParserType,
                        spreadsheet.getInputStream());
                addMessage("Uploaded " + mapBarcodeToPlate.size() + " plates");
            } catch (Exception e) {
                addGlobalValidationError(e.getMessage());
            }
        }
        return new ForwardResolution(MOL_IND_PLATE_PAGE);
    }

    @DefaultHandler
    @HandlesEvent(MAKE_DEFINITION)
    public Resolution makeDefinition() {
        plateGeometry = null;
        reagentType = null;
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    @HandlesEvent(UPLOAD_DEFINITION)
    public Resolution uploadDef() {
        if (plateName.length() > 254) {
            addValidationError(plateName, "Plate name is longer than 254 characters.");
            return new ForwardResolution(PLATE_DEFINITION_PAGE);
        }
        List<List<String>> rows = null;
        try {
            // Validates the spreadsheet and creates a Index Plate Definition.
            rows = indexedPlateFactory.parseSpreadsheet(spreadsheet.getInputStream(), 2, messageCollection);
        } catch (IOException e) {
            messageCollection.addError("Cannot read file: " + e.toString());
        }
        if (!messageCollection.hasErrors()) {
            VesselGeometry vesselGeometry = plateGeometry.equals("96") ? VesselGeometry.G12x8 : VesselGeometry.G24x16;
            indexedPlateFactory.makeIndexPlateDefinition(plateName, rows, vesselGeometry,
                    IndexPlateDefinition.ReagentType.valueOf(reagentType), allowOverwrite, messageCollection);
        }
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    @HandlesEvent(MAKE_INSTANCE)
    public Resolution makeInstance() {
        plateNames = indexedPlateFactory.findPlateDefinitionNames();
        salesOrderNumber = null;
        plateName = null;
        return new ForwardResolution(PLATE_INSTANCE_PAGE);
    }

    @HandlesEvent(FIND_LAYOUT)
    public Resolution findLayout() {
        // Populates the plateLayout.
        plateLayout = new ArrayList<>();
        indexedPlateFactory.findLayout(plateName, plateLayout, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_INSTANCE_PAGE);
    }

    @HandlesEvent(CREATE_INSTANCE)
    public Resolution createInstance() {
        // Creates index plates from the barcodes in the spreadsheet, with content defined by the Index Plate Definition.
        List<List<String>> rows = null;
        try {
            rows = indexedPlateFactory.parseSpreadsheet(spreadsheet.getInputStream(), 1, messageCollection);
        } catch (IOException e) {
            messageCollection.addError("Cannot read file: " + e.toString());
        }
        if (!messageCollection.hasErrors()) {
            indexedPlateFactory.makeIndexPlate(plateName, rows, messageCollection);
        }
        addMessages(messageCollection);
        return makeInstance();
    }

    @HandlesEvent(MANAGE_DEFINITION)
    public Resolution manageDefinition() {
        plateNames = indexedPlateFactory.findPlateDefinitionNames();
        return new ForwardResolution(PLATE_DEFINITION_MANAGEMENT_PAGE);
    }

    public List<String> getReagentTypes() {
        return Stream.of(IndexPlateDefinition.ReagentType.values()).
                map(type -> type.name()).
                sorted().collect(Collectors.toList());
    }

    public String getPlateGeometry() {
        return plateGeometry;
    }

    public void setPlateGeometry(String plateGeometry) {
        this.plateGeometry = plateGeometry;
    }

    public String getReagentType() {
        return reagentType;
    }

    public void setReagentType(String reagentType) {
        this.reagentType = reagentType;
    }

    public String getSalesOrderNumber() {
        return salesOrderNumber;
    }

    public void setSalesOrderNumber(String salesOrderNumber) {
        this.salesOrderNumber = salesOrderNumber;
    }

    public boolean isAllowOverwrite() {
        return allowOverwrite;
    }

    public void setAllowOverwrite(boolean allowOverwrite) {
        this.allowOverwrite = allowOverwrite;
    }

    public String getPlateName() {
        return plateName;
    }

    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    public FileBean getSpreadsheet() {
        return spreadsheet;
    }

    public List<String> getPlateNames() {
        return plateNames;
    }

    public void setPlateNames(List<String> plateNames) {
        this.plateNames = plateNames;
    }

    public List<List<String>> getPlateLayout() {
        return plateLayout;
    }

    public void setPlateLayout(List<List<String>> plateLayout) {
        this.plateLayout = plateLayout;
    }

    public List<String> getPlateBarcodes() {
        return plateBarcodes;
    }

    public void setPlateBarcodes(List<String> plateBarcodes) {
        this.plateBarcodes = plateBarcodes;
    }

    public void setSpreadsheet(FileBean spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public void setTechnologyAndParserType(
            IndexedPlateFactory.TechnologiesAndParsers technologyAndParserType) {
        this.technologyAndParserType = technologyAndParserType;
    }

    public IndexedPlateFactory.TechnologiesAndParsers[] getTechnologiesAndParsers() {
        return IndexedPlateFactory.TechnologiesAndParsers.values();
    }

    public List<IndexedPlateFactory.TechnologiesAndParsers> getActiveTechnologiesAndParsers() {
        List<IndexedPlateFactory.TechnologiesAndParsers> activeParsers = new ArrayList<>();
        IndexedPlateFactory.TechnologiesAndParsers[] values = IndexedPlateFactory.TechnologiesAndParsers.values();
        for (IndexedPlateFactory.TechnologiesAndParsers technologiesAndParser: values) {
            if (technologiesAndParser.isActive()) {
                activeParsers.add(technologiesAndParser);
            }
        }
        return activeParsers;
    }
}
