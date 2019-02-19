package org.broadinstitute.gpinformatics.mercury.boundary.storage;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private String logDirName = System.getProperty("jboss.server.log.dir");

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

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/CreateStorageLocation.json, so it can
     * be used for other similar fixups, without writing a new test. The json object is the CreateStorageLocation.class:
     */
    @Test(enabled = false)
    public void fixupGplim6012BackfillInPlaceRacks() throws Exception {

        File logFile = new File(logDirName, "fixupGplim6012BackfillInPlaceRacks.log");
        PrintWriter logWriter = new PrintWriter( new FileWriter(logFile, false) );
        // Not all columns used - added for pre and post debugging queries
        String query =   "SELECT e.in_place_lab_vessel AS formation_id,\n"
                       + "       tf.label AS formation_barcode,\n"
                       + "       e.lab_event_id,\n"
                       + "       e.lab_event_type,\n"
                       + "       e.event_date,\n"
                       + "       COUNT(e.lab_event_id) OVER ( PARTITION BY e.in_place_lab_vessel ) AS event_count,\n"
                       + "       COUNT(rot.racks_of_tubes) AS rack_count,\n"
                       + "       MAX( r.lab_vessel_id ) AS tf_rack2use_if_single,\n"
                       + "       MAX( CASE WHEN r.storage_location IS NULL THEN 0 ELSE r.lab_vessel_id END ) AS st_rack2use_if_single,\n"
                       + "       SUM( CASE WHEN r.storage_location IS NULL THEN 0 ELSE 1 END ) AS storage_count,\n"
                       + "       LISTAGG( r.label, ',' ) WITHIN GROUP (ORDER BY r.label) AS racks,\n"
                       + "       a.label as assigned_rack\n"
                       + "  FROM lab_event e,\n"
                       + "       lab_vessel tf,\n"
                       + "       lab_vessel_racks_of_tubes rot,\n"
                       + "       lab_vessel r,\n"
                       + "       lab_vessel a\n"
                       + " WHERE e.in_place_lab_vessel IS NOT NULL\n"
                       //+ "   AND e.ancillary_in_place_vessel IS NULL\n" /* *** TEST RE-RUNS - BE CAREFUL! *** */
                       + "   AND e.in_place_lab_vessel = tf.lab_vessel_id\n"
                       + "   AND tf.dtype = 'TubeFormation'\n"
                       + "   AND e.lab_event_type = 'STORAGE_CHECK_IN'\n"
                       + "   AND tf.lab_vessel_id = rot.lab_vessel\n"
                       + "   AND r.lab_vessel_id = rot.racks_of_tubes\n"
                       + "   --AND e.in_place_lab_vessel = 5057569\n"   /* *** TESTING *** */
                       //+ "   AND e.event_date < TO_DATE( '12/20/2018', 'mm/dd/yyyy')\n" /* *** TODO: Exclude BSP migrations - REMOVE *** */
                       + "   AND e.ancillary_in_place_vessel = a.lab_vessel_id(+)\n" /* Outer join */
                       + "GROUP BY e.in_place_lab_vessel, tf.label, e.lab_event_id, e.lab_event_type, e.event_date, a.label\n"
                       + "ORDER BY e.in_place_lab_vessel, e.lab_event_id";

        userBean.loginOSUser();
        utx.begin();

        logWriter.println("***** STARTING *****" );
        logWriter.println(SimpleDateFormat.getDateTimeInstance().format(new Date()));

        EntityManager em = storageLocationDao.getEntityManager();
        Query qry = em.createNativeQuery(query);
        // Only 371 rows as of 12/20/2018
        List<Object[]> rslts = qry.getResultList();
        // Add edge control
        rslts.add( new Object[]{ Long.MAX_VALUE, "", 0L, "", null, 0L, 0L, 0L, 0L, 0L, "", ""});
        // ****** Result set structure
        //Long formationId = (Long) row[0];                  e.in_place_lab_vessel as formation_id
        //String formationBarcode = (String) row[1];         tf.label as formation_barcode
        //Long labEventId = (Long) row[2];                   e.lab_event_id
        //String labEventType = (String) row[3];             e.lab_event_type
        //java.sql.Date eventDate = (java.sql.Date) row[4];  e.event_date
        //Long eventCount = (Long) row[5];                   count(e.lab_event_id) over ( partition by e.in_place_lab_vessel )  as event_count
        //Long rackCount = (Long) row[6];                    count(rot.racks_of_tubes) as rack_count
        //Long tfRackId = (Long) row[7];                     max( r.lab_vessel_id ) as tf_rack2use_if_single
        //Long stRackId = (Long) row[8];                     MAX( CASE WHEN r.storage_location IS NULL THEN 0 ELSE r.lab_vessel_id END ) AS st_rack2use_if_single
        //Long storageLocationCount = (Long) row[9];         sum( case when r.storage_location is null then 0 else 1 end ) as storage_count
        //String rackBarcodes = (String) row[10];            listagg( r.label, ',' ) within group (order by r.label) as racks
        //String rackAssigned = (String) row[11];            a.label as assigned_rack

        // First pass for tube formations with only a single rack assigned - assign it to all events with same tube formation
        rslts.stream().filter( row -> new Long(row[6].toString()) == 1 ).forEach((row) -> {
            Long formationId = new Long(row[0].toString());
            String formationBarcode = (String) row[1];
            Long labEventId = new Long(row[2].toString());
            Long tfRackId = new Long(row[7].toString());
            String rackBarcodes = (String) row[10];
            if( formationId < Long.MAX_VALUE ) {
                assignRackToEvent(labEventId, tfRackId);
                logWriter.println("The single rack " + rackBarcodes + " for formation " + formationBarcode
                                  + " was assigned to event " + labEventId);
            }
        });

        // Second pass for tube formations with none or only a single rack in storage and only one event available
        rslts.stream().filter( row -> new Long(row[6].toString()) > 1 && new Long(row[9].toString()) <= 1 ).forEach((row) -> {
            Long formationId = new Long(row[0].toString());
            String formationBarcode = (String) row[1];
            Long labEventId = new Long(row[2].toString());
            Long eventCount = new Long(row[5].toString());
            Long stRackId = new Long(row[8].toString());
            Long storageLocationCount = new Long(row[9].toString());
            String rackBarcodes = (String) row[10];
            if (formationId < Long.MAX_VALUE && eventCount == 1) {
                assignRackToEvent(labEventId, stRackId);
                if( storageLocationCount == 1 ) {
                    logWriter.println("\tThe only rack in storage from " + rackBarcodes + " for formation " + formationBarcode
                                      + " was assigned to event " + labEventId );
                } else {
                    // No idea which to assign to event, rather leave it null than guess
                    logWriter.println("\tNone of the racks are in storage from " + rackBarcodes + " for formation " + formationBarcode
                                      + " - was assigned to event " + labEventId );
                }
            }
         });

        // Third pass is too gnarly - tube formations have multiple racks, zero or more locations, multiple events
        // 89ba34c873166f549938e4f67fc01e6b 0000007863318 not stored
        assignRackToEvent(2445597L, "000007863318");
        assignRackToEvent(2445601L, "000007863318");
        // 4984d49c44276e567cfa2d04e40046f8  CO-26240832 not stored
        assignRackToEvent(2700322L, "CO-26243836");
        assignRackToEvent(2700509L, "CO-26243836");
        //ace33f7bc43eebcadecf19926b4f7362
        assignRackToEvent(2749690L, "CO-26391033"); // CO-26391033 overwritten by CO-26205654 - added one tube
        // bb961b7dce0591fb15d611c80868bce4
            // 3 newest events all within 4 minutes - rack CO-26379405 layout in storage matches formation
            assignRackToEvent(2993888L, "CO-26379405");
            assignRackToEvent(2993893L, "CO-26379405");
            assignRackToEvent(2993894L, "CO-26379405");
            // Old events - rack CO-25344129 reused for different formation storage
            assignRackToEvent(2773314L, "CO-25344129");
            assignRackToEvent(2777869L, "CO-25344129");
        // 6a91871544f174680838204b57fb8a3e - both overwritten by a newer event on rack
        assignRackToEvent(2796589L, "CO-26641653");
        assignRackToEvent(2813519L, "CO-26641653");

        // 815572fb1d15232f3598c40b4ef49d16 use the only rack created before event
        assignRackToEvent(2885563L, "CO-26769506" );

        // ff4d896cf506b768ffe31017059e5c9e Not even in storage any longer, use only rack created before event
        assignRackToEvent(2924300L, "CO-26863282");
        assignRackToEvent(2925482L, "CO-26863282");
        assignRackToEvent(2925485L, "CO-26863282");

        storageLocationDao.persist(new FixupCommentary("GPLIM-6012 org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageLocationFixupTest.fixupGplim6012BackfillInPlaceRacks()"));
        logWriter.println("***** Flushing persistence context *****" );
        logWriter.println(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        storageLocationDao.flush();
        utx.commit();

        logWriter.println("***** FINISHED *****" );
        logWriter.println(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        logWriter.flush();
        logWriter.close();
    }

    private void assignRackToEvent( Long labEventId, Long rackId ) {
        LabEvent event = storageLocationDao.findSingle(LabEvent.class, LabEvent_.labEventId, labEventId);
        LabVessel rack = storageLocationDao.findSingle(LabVessel.class, LabVessel_.labVesselId, rackId);
        event.setAncillaryInPlaceVessel(rack);
    }

    private void assignRackToEvent( Long labEventId, String rackBarcode ) {
        LabEvent event = storageLocationDao.findSingle(LabEvent.class, LabEvent_.labEventId, labEventId);
        LabVessel rack = storageLocationDao.findSingle(LabVessel.class, LabVessel_.label, rackBarcode);
        event.setAncillaryInPlaceVessel(rack);
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
