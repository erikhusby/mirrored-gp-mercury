package org.broadinstitute.gpinformatics.mercury.boundary.storage;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to StorageLocation entities
 */
@Test(groups = TestGroups.FIXUP)
public class StorageLocationFixupTest extends Arquillian {

    @Inject
    StorageLocationDao storageLocationDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/CreateStorageLocation.xml, so it can
     * be used for other similar fixups, without writing a new test. The XML jaxb object is :
     * GPLIM-4104
     * InfiniumHybridization
     * 1278705
     * 1278706
     * 1278707
     */
    @Test(enabled = false)
    public void fixupGplim4178InitialStorageEntry() throws IOException {
        InputStream testResource = VarioskanParserTest.getTestResource("CreateStorageLocation.json");
        ObjectMapper mapper = new ObjectMapper();
        StorageLocationDto[] dtos = mapper.readValue(testResource, StorageLocationDto[].class);
        List<StorageLocation> topLevelLocations = new ArrayList<>();
        for (StorageLocationDto dto: dtos) {
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

        storageLocationDao.persist(new FixupCommentary(""));
        storageLocationDao.persistAll(topLevelLocations);
    }

    private static StorageLocation buildStorageLocation(StorageLocation parent, StorageLocationDto dto) {
        StorageLocation.LocationType locationType = StorageLocation.LocationType.getByDisplayName(
                dto.getLocationType());
        StorageLocation storageLocation = new StorageLocation(dto.getLabel(), locationType, parent);
        for (StorageLocationDto childDto: dto.getChildren()) {
            StorageLocation childStorageLocation = buildStorageLocation(storageLocation, childDto);
            storageLocation.getChildrenStorageLocation().add(childStorageLocation);
        }
        return storageLocation;
    }

    public static class StorageLocationDto {
        private String locationType;
        private String label;
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
