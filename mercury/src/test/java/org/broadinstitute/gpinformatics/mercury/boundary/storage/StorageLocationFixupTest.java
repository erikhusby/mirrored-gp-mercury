package org.broadinstitute.gpinformatics.mercury.boundary.storage;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to StorageLocation entities
 */
@Test(groups = TestGroups.FIXUP)
public class StorageLocationFixupTest extends Arquillian {

    @Inject
    StorageLocationDao storageLocationDao;

    @Inject
    private UserBean userBean;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/CreateStorageLocation.json, so it can
     * be used for other similar fixups, without writing a new test. The json object is the CreateStorageLocation.class:
     */
    @Test(enabled = false)
    public void fixupGplim4178InitialStorageEntry() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        InputStream testResource = VarioskanParserTest.getTestResource("CreateStorageLocation.json");
        ObjectMapper mapper = new ObjectMapper();
        CreateStorageLocation createStorageLocation = mapper.readValue(testResource, CreateStorageLocation.class);
        if (StringUtils.isEmpty(createStorageLocation.getFixupCommentary())) {
            throw new RuntimeException("Must provide a fixup commentary");
        }
        List<StorageLocation> topLevelLocations = new ArrayList<>();
        for (StorageLocationDto dto: createStorageLocation.getStorageLocations()) {
            StorageLocation.LocationType locationType = StorageLocation.LocationType.getByDisplayName(
                    dto.getLocationType());
            switch (locationType) {
            case REFRIGERATOR:
            case FREEZER:
            case SHELVINGUNIT:
            case CABINET:
                StorageLocation storageLocation = buildStorageLocation(null, dto);
                topLevelLocations.add(storageLocation);
                break;
            default:
                throw new RuntimeException("This fixup is only meant to create top level locations.");
            }
        }

        storageLocationDao.persist(new FixupCommentary(createStorageLocation.getFixupCommentary()));
        storageLocationDao.persistAll(topLevelLocations);
        storageLocationDao.flush();
        utx.commit();
    }

    private static StorageLocation buildStorageLocation(StorageLocation parent, StorageLocationDto dto) {
        StorageLocation.LocationType locationType = StorageLocation.LocationType.getByDisplayName(
                dto.getLocationType());
        StorageLocation storageLocation = new StorageLocation(dto.getLabel(), locationType, parent);
        storageLocation.setBarcode(dto.getBarcode());
        for (StorageLocationDto childDto: dto.getChildren()) {
            StorageLocation childStorageLocation = buildStorageLocation(storageLocation, childDto);
            storageLocation.getChildrenStorageLocation().add(childStorageLocation);
        }
        return storageLocation;
    }

    public static class CreateStorageLocation {
        private String fixupCommentary;
        private List<StorageLocationDto> storageLocations;

        public String getFixupCommentary() {
            return fixupCommentary;
        }

        public void setFixupCommentary(String fixupCommentary) {
            this.fixupCommentary = fixupCommentary;
        }

        public List<StorageLocationDto> getStorageLocations() {
            return storageLocations;
        }

        public void setStorageLocations(
                List<StorageLocationDto> storageLocations) {
            this.storageLocations = storageLocations;
        }
    }

    public static class StorageLocationDto {
        private String locationType;
        private String label;
        private String barcode;
        private List<StorageLocationDto> children;

        public String getLocationType () {
            return locationType;
        }

        public void setLocationType (String locationType) {
            this.locationType = locationType;
        }

        public String getLabel () {
            return label;
        }

        public void setLabel (String label) {
            this.label = label;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public List<StorageLocationDto> getChildren() {
            if (children == null) {
                children = new ArrayList<>();
            }
            return children;
        }

        public void setChildren(
                List<StorageLocationDto> children) {
            this.children = children;
        }
    }
}
