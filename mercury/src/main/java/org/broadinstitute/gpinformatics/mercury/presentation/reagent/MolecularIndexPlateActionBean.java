package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
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

    public static final String PLATE_DEFINITION_PAGE = "/reagent/index_plate_define.jsp";
    public static final String DEFINITION_ACTION = "indexDefinition";
    public static final String FIND_LAYOUT_D = "findLayoutD";
    public static final String CREATE_DEFINITION = "createDefinition";
    public static final String RENAME_DEFINITION = "renameDefinition";
    public static final String DELETE_DEFINITION = "deleteDefinition";
    public static final String FIND_INSTANCES = "findInstances";

    public static final String PLATE_INSTANCE_PAGE = "/reagent/index_plate_instance.jsp";
    public static final String INSTANCE_ACTION = "indexInstance";
    public static final String FIND_LAYOUT_I = "findLayoutI";
    public static final String CREATE_INSTANCE = "createInstance";
    public static final String DELETE_INSTANCES = "deleteInstances";
    public static final String FIND_DEFINITIONS = "findDefinitions";

    @Validate(required = true, on = {CREATE_DEFINITION})
    private String plateGeometry;

    @Validate(required = true, on = {CREATE_DEFINITION})
    private String reagentType;

    @Validate(required = true, on = {CREATE_INSTANCE})
    private String salesOrderNumber;

    private boolean replaceExisting;

    @Validate(required = true, on = {CREATE_DEFINITION})
    private String plateName;

    @Validate(required = true, on = {RENAME_DEFINITION, DELETE_DEFINITION, FIND_INSTANCES, CREATE_INSTANCE})
    private String selectedPlateName;

    @Validate(required = true, on = {CREATE_DEFINITION, CREATE_INSTANCE})
    private FileBean spreadsheet;

    @Validate(required = true, on = {RENAME_DEFINITION})
    private String newDefinitionName;

    @Validate(required = true, on = {FIND_DEFINITIONS, DELETE_INSTANCES})
    private String plateBarcodeString;

    private MessageCollection messageCollection = new MessageCollection();
    private List<String> plateNames = new ArrayList<>();
    private List<List<String>> plateLayout = new ArrayList<>();
    private List<String> plateBarcodes = new ArrayList<>();
    private List<Pair<String, String>> definitionToBarcode;
    private Boolean isManagePage;
    private Pair<String, String> unusedAndInUse;

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
    @HandlesEvent(DEFINITION_ACTION)
    public Resolution definitionAction() {
        plateNames = indexedPlateFactory.findPlateDefinitionNames();
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    /** Creates an Index Plate Definition from the spreadsheet. */
    @HandlesEvent(CREATE_DEFINITION)
    public Resolution createDefinition() {
        // Strips out the non-7-bit ascii, control characters, and extra spaces.
        plateName = StringUtils.normalizeSpace(MercuryStringUtils.cleanupValue(plateName));
        if (StringUtils.isBlank(plateName)) {
            addGlobalValidationError("Plate name is blank.");
        } else if (plateName.length() > 254) {
            addGlobalValidationError("Plate name is longer than 254 characters.");
        } else {
            List<List<String>> rows = null;
            try {
                rows = indexedPlateFactory.parseSpreadsheet(spreadsheet.getInputStream(), 2, messageCollection);
            } catch (IOException e) {
                messageCollection.addError("Cannot read file: " + e.toString());
            }
            if (!messageCollection.hasErrors()) {
                VesselGeometry vesselGeometry = plateGeometry.equals("96") ? VesselGeometry.G12x8 :
                        VesselGeometry.G24x16;
                indexedPlateFactory.makeIndexPlateDefinition(plateName, rows, vesselGeometry,
                        IndexPlateDefinition.ReagentType.valueOf(reagentType), replaceExisting, messageCollection);
                plateNames = indexedPlateFactory.findPlateDefinitionNames();
            }
        }
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    @HandlesEvent(INSTANCE_ACTION)
    public Resolution makeInstance() {
        plateNames = indexedPlateFactory.findPlateDefinitionNames();
        return new ForwardResolution(PLATE_INSTANCE_PAGE);
    }

    @HandlesEvent(FIND_LAYOUT_D)
    public Resolution findLayoutD() {
        return findLayout(true);
    }

    @HandlesEvent(FIND_LAYOUT_I)
    public Resolution findLayoutI() {
        return findLayout(false);
    }

    private Resolution findLayout(boolean isDefPage) {
        // Populates the plateLayout.
        plateLayout = new ArrayList<>();
        indexedPlateFactory.findLayout(selectedPlateName, plateLayout, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(isDefPage ? PLATE_DEFINITION_PAGE : PLATE_INSTANCE_PAGE);
    }

    /**
     * Creates index plates from an index plate definition.
     */
    @HandlesEvent(CREATE_INSTANCE)
    public Resolution createInstance() {
        List<List<String>> rows = null;
        try {
            rows = indexedPlateFactory.parseSpreadsheet(spreadsheet.getInputStream(), 1, messageCollection);
        } catch (IOException e) {
            messageCollection.addError("Cannot read file: " + e.toString());
        }
        if (!messageCollection.hasErrors()) {
            indexedPlateFactory.makeIndexPlate(selectedPlateName, rows, salesOrderNumber, replaceExisting,
                    messageCollection);
            addMessages(messageCollection);
        }
        return new ForwardResolution(PLATE_INSTANCE_PAGE);
    }

    @HandlesEvent(RENAME_DEFINITION)
    public Resolution renameDefinition() {
        newDefinitionName = MercuryStringUtils.cleanupValue(newDefinitionName);
        if (StringUtils.isBlank(newDefinitionName)) {
            addGlobalValidationError("New plate name is blank.");
        } else if (newDefinitionName.length() > 254) {
            addGlobalValidationError("New plate name is longer than 254 characters.");
        } else {
            indexedPlateFactory.renameDefinition(selectedPlateName, newDefinitionName, messageCollection);
            if (!messageCollection.hasErrors()) {
                plateNames = indexedPlateFactory.findPlateDefinitionNames();
            }
        }
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    @HandlesEvent(DELETE_DEFINITION)
    public Resolution deleteDefinition() {
        indexedPlateFactory.deleteDefinition(selectedPlateName, messageCollection);
        if (!messageCollection.hasErrors()) {
            plateNames = indexedPlateFactory.findPlateDefinitionNames();
        }
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    @HandlesEvent(FIND_INSTANCES)
    public Resolution findInstances() {
        unusedAndInUse = indexedPlateFactory.findInstances(selectedPlateName, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_DEFINITION_PAGE);
    }

    @HandlesEvent(FIND_DEFINITIONS)
    public Resolution findDefinitions() {
        plateBarcodes = splitCleanedUnique(plateBarcodeString);
        if (plateBarcodes.isEmpty()) {
            addGlobalValidationError("Plate barcodes are required.");
        } else {
            definitionToBarcode = indexedPlateFactory.findDefinitions(plateBarcodes);
        }
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_INSTANCE_PAGE);
    }

    @HandlesEvent(DELETE_INSTANCES)
    public Resolution deleteInstances() {
        plateBarcodes = splitCleanedUnique(plateBarcodeString);
        if (plateBarcodes.isEmpty()) {
            addGlobalValidationError("Plate barcodes are required.");
        } else {
            indexedPlateFactory.deleteInstances(plateBarcodes, messageCollection);
        }
        addMessages(messageCollection);
        return new ForwardResolution(PLATE_INSTANCE_PAGE);
    }

    private List<String> splitCleanedUnique(String inputString) {
        return Stream.of(StringUtils.split(inputString)).
                map(MercuryStringUtils::cleanupValue).
                map(StringUtils::normalizeSpace).
                filter(StringUtils::isNotBlank).
                distinct().
                collect(Collectors.toList());
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

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
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

    public List<Pair<String, String>> getDefinitionToBarcode() {
        return definitionToBarcode;
    }

    public void setDefinitionToBarcode(List<Pair<String, String>> definitionToBarcode) {
        this.definitionToBarcode = definitionToBarcode;
    }

    public String getNewDefinitionName() {
        return newDefinitionName;
    }

    public void setNewDefinitionName(String newDefinitionName) {
        this.newDefinitionName = newDefinitionName;
    }

    public String getPlateBarcodeString() {
        return plateBarcodeString;
    }

    public void setPlateBarcodeString(String plateBarcodeString) {
        this.plateBarcodeString = plateBarcodeString;
    }

    public Pair<String, String> getUnusedAndInUse() {
        return unusedAndInUse;
    }

    public void setUnusedAndInUse(Pair<String, String> unusedAndInUse) {
        this.unusedAndInUse = unusedAndInUse;
    }

    public Boolean getManagePage() {
        return isManagePage;
    }

    public void setManagePage(Boolean managePage) {
        isManagePage = BooleanUtils.isTrue(managePage);
    }

    public void setTechnologyAndParserType(
            IndexedPlateFactory.TechnologiesAndParsers technologyAndParserType) {
        this.technologyAndParserType = technologyAndParserType;
    }

    public String getSelectedPlateName() {
        return selectedPlateName;
    }

    public void setSelectedPlateName(String selectedPlateName) {
        this.selectedPlateName = selectedPlateName;
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
