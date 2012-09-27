package org.broadinstitute.gpinformatics.mercury.integration.web.zims;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.LoadableComponent;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElement;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author breilly
 */
public class IlluminaRunQueryPage extends LoadableComponent<IlluminaRunQueryPage> {

    public static final String RUN_NAME_ID = "form:runNameOutput";
    public static final String RUN_DATE_ID = "form:runDateOutput";
    public static final String LAST_CYCLE_ID = "form:lastCycleOutput";
    public static final String MOLECULAR_BARCODE_CYCLE_ID = "form:molecularBarcodeCycleOutput";
    public static final String MOLECULAR_BARCODE_LENGTH_ID = "form:molecularBarcodeLengthOutput";
    public static final String LANE_DETAIL_HEADER_ID = "form:libraryTable:laneDetailsHeader";

    private WebDriver driver;
    private URL baseUrl;
    private WebDriverWait wait;

    @FindBy(id = "runName")
    private WebElement runNameInput;

    @FindBy(id = "queryButton")
    private WebElement queryButton;

    @FindBy(id = RUN_NAME_ID)
    private WebElement runName;

    @FindBy(id = "form:runBarcodeOutput")
    private WebElement runBarcode;

    @FindBy(id = "form:sequencerOutput")
    private WebElement sequencer;

    @FindBy(id = "form:sequencerModelOutput")
    private WebElement sequencerModel;

    @FindBy(id = "form:flowcellBarcodeOutput")
    private WebElement flowcellBarcode;

    @FindBy(id = "form:pairedOutput")
    private WebElement paired;

    @FindBy(id = "form:firstCycleOutput")
    private WebElement firstCycle;

    @FindBy(id = "form:firstCycleReadLengthOutput")
    private WebElement firstCycleReadLength;

    @FindBy(id = "form:lanes_data")
//    @CacheLookup
    private WebElement lanesData;

    @FindBy(id = "form:libraryTable")
//    @CacheLookup
    private WebElement libraryTable;

    @FindBy(id = "form:libraryTable_data")
//    @CacheLookup
    private WebElement libraryData;

    @FindBy(xpath = "//div[@id='form:libraryTable']//thead/tr[2]/th")
//    @CacheLookup
    private List<WebElement> libraryHeaders;

    @FindBy(xpath = "//tbody[@id='form:libraryTable_data']/tr")
//    @CacheLookup
    private List<WebElement> libraryDataRows;

    private Map<Integer, List<WebElement>> libraryRowCells = new HashMap<Integer, List<WebElement>>();

    @FindBy(id = "reads_data")
//    @CacheLookup
    private WebElement readsData;

    public IlluminaRunQueryPage(WebDriver driver, URL baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        wait = new WebDriverWait(driver, 10);
    }

    @Override
    protected void load() {
        String url = baseUrl + "index.xhtml";
        driver.get(url);
        wait.until(titleIs("Mercury : Home"));
        navigate("Illumina Run Query");
        wait.until(titleIs("Mercury : Illumina Run Query"));
        initElements();
    }

    private void initElements() {
        PageFactory.initElements(driver, this);
        libraryRowCells.clear();
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getTitle(), "Mercury : Illumina Run Query");
    }

    public void enterRunName(String runName) {
//        driver.findElement(By.id(RUN_NAME_INPUT_ID)).sendKeys(runName);
        runNameInput.sendKeys(runName);
    }

    public void submitQueryAndWait() {
//        driver.findElement(By.id(QUERY_BUTTON_ID)).click();
        queryButton.click();
        wait.until(presenceOfElementLocated(By.id(RUN_NAME_ID)));
        initElements();
    }

    public String getRunName() {
        return runName.getText();
    }

    public String getRunBarcode() {
        return runBarcode.getText();
    }

    public String getRunDate() {
        return getText(RUN_DATE_ID);
    }

    public String getSequencer() {
        return sequencer.getText();
    }

    public String getSequencerModel() {
        return sequencerModel.getText();
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode.getText();
    }

    public String getPaired() {
        return paired.getText();
    }

    public String getFirstCycle() {
        return firstCycle.getText();
    }

    public String getFirstCycleReadLength() {
        return firstCycleReadLength.getText();
    }

    public String getLastCycle() {
        return getText(LAST_CYCLE_ID);
    }

    public String getMolecularBarcodeCycle() {
        return getText(MOLECULAR_BARCODE_CYCLE_ID);
    }

    public String getMolecularBarcodeLength() {
        return getText(MOLECULAR_BARCODE_LENGTH_ID);
    }

    public int getNumLanes() {
        return getRows(lanesData).size();
    }

    public String getLaneNumber(int lane) {
        return getCells(lanesData, lane).get(0).getText();
    }

    public String getPrimer(int lane) {
        return getCells(lanesData, lane).get(1).getText();
    }

    public String getNumLibraries(int lane) {
        return getCells(lanesData, lane).get(2).getText();
    }

    public void selectLaneAndWait(final int lane) {
        getCells(lanesData, lane).get(0).findElement(By.tagName("div")).click();
/*
        wait.until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return getLaneDetailsHeader().equals("Lane " + (lane + 1) + " Details");
            }
        });
*/
        wait.until(textToBePresentInElement(By.id(LANE_DETAIL_HEADER_ID), "Lane " + (lane + 1) + " Details"));
        initElements();
    }

    public String getLaneDetailsHeader() {
        return getText(LANE_DETAIL_HEADER_ID);
    }

    public String getLaneDetailFirstCell() {
        return getCells(libraryData, 0).get(0).getText();
    }

    public List<String> getLibraryColumns() {
//        List<WebElement> headers = driver.findElement(By.id(LIBRARY_TABLE_ID)).findElements(By.tagName("tr")).get(1).findElements(By.tagName("th"));
        List<WebElement> headers = libraryTable.findElements(By.tagName("tr")).get(1).findElements(By.tagName("th"));
        List<String> columns = new ArrayList<String>();
        for (WebElement header : headers) {
            columns.add(header.getText());
        }
        return columns;
    }

    public void toggleColumn(final String column) {
        final boolean activeBefore = driver.findElement(By.tagName("body")).getText().contains(column);
        WebElement menu = driver.findElement(By.id("form:libraryTable:laneDetailColumns")).findElement(By.className("ui-selectcheckboxmenu-trigger"));
        menu.click();
        WebElement columnList = wait.until(presenceOfElementLocated(By.id("form:libraryTable:laneDetailColumns_panel")));
        List<WebElement> columnOptions = columnList.findElements(By.tagName("li"));
        for (WebElement columnOption : columnOptions) {
            WebElement label = columnOption.findElement(By.tagName("label"));
            if (label.getText().equals(column)) {
                label.click();
                break;
            }
        }
        wait.until(new ExpectedCondition<Object>() {
            @Override
            public Object apply(WebDriver webDriver) {
                return driver.findElement(By.tagName("body")).getText().contains(column) != activeBefore;
            }
        });
        initElements();
    }

    public int getNumLibraryDatas() {
//        return getRows(LIBRARY_DATA_ID).size();
//        return getRows(libraryData).size();
        return libraryDataRows.size();
    }

    public String getLibraryData(int row, String column) {
//        return getCells(LIBRARY_DATA_ID, row).get(getLibraryColumns().indexOf(column)).getText();
//        return getCells(libraryData, row).get(getLibraryColumns().indexOf(column)).getText();
        List<WebElement> cells = libraryRowCells.get(row);
        if (cells == null) {
            cells = getCells(libraryDataRows.get(row));
            libraryRowCells.put(row, cells);
        }
        return cells.get(getLibraryColumns().indexOf(column)).getText();
    }

    public int getNumReads() {
        return getRows(readsData).size();
    }

    public String getReadType(int read) {
        return getCells(readsData, read).get(0).getText();
    }

    public String getReadFirstCycle(int read) {
        return getCells(readsData, read).get(1).getText();
    }

    public String getReadLength(int read) {
        return getCells(readsData, read).get(2).getText();
    }

    // general utilities

    private String getText(String elementId) {
        return driver.findElement(By.id(elementId)).getText();
    }

    private List<WebElement> getRows(String tableId) {
        return driver.findElement(By.id(tableId)).findElements(By.tagName("tr"));
    }

    private List<WebElement> getRows(WebElement table) {
        return table.findElements(By.tagName("tr"));
    }

    private List<WebElement> getCells(String tableId, int row) {
        return getRows(tableId).get(row).findElements(By.tagName("td"));
    }

    private List<WebElement> getCells(WebElement table, int row) {
        return getRows(table).get(row).findElements(By.tagName("td"));
    }

    private List<WebElement> getCells(WebElement row) {
        return row.findElements(By.tagName("td"));
    }

    private void navigate(String linkText) {
        driver.findElement(By.linkText(linkText)).click();
/*
        try {
            wait.until(titleIs("Mercury : " + linkText));
        } catch (TimeoutException e) {
            fail("Timeout waiting for page title: '" + linkText + "'. Current page title is: '" + driver.getTitle() + "'.", e);
        }
*/
    }
}
