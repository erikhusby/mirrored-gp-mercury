package org.broadinstitute.gpinformatics.mercury.control.labevent;

/**
 * Test the ability to record manual transfers, through a web page.
 */
public class ManualTransferTest {

    public void testSectionTransfer() {
        ManualTransferFactory manualTransferFactory = new ManualTransferFactory();
        // Should the web page use AJAX to build dot notation that is created by Stripes on final POST,
        // or do POSTS for each action, and accumulate results in JAXB DTO?.
        //      DTO minimizes amount of JavaScript, but the repaint may confuse the user.
        //      DTO would still have to be constructed by Stripes, unless stored in session.
        //      Could store DTO in preference, which would allow saving, and avoiding losing data due to session timeout.
        //      User defined search uses AJAX, but it deals with SearchTerm only, whereas this page would deal with
        //      ~10 types, which would lead to a proliferation of JSP fragments
        //           plate, positionMap, receptacle, reagent, metadata, cherry pick source
        //      these fragments may be necessary anyway, to reduce duplication

        // ignore multiple events per message for now

        // ReceptacleEvent
        // PlateEvent
        // PlateTransferEvent
        // PlateCherryPickEvent
        // ReceptaclePlateTransferEvent
        // No JAXB representation for tube to tube transfer?  Create ReceptacleTransferEvent?

        // receptacle type

        // GET: enter page
        // render drop downs of lab event types, machine names
        // user chooses lab event type (set mode mercury), clicks button
        // POST: render skeleton of section message (or cherry, tube to tube) including default reagents and default vessel types
        // user scans rack barcode
        // user clicks rack scan button (manual alternative?)
        // AJAX: render tube barcodes in geometry, with volume text box
        // user chooses source section
        // user sets volumes (need bulk change)
        // user scans destination plate
        // AJAX: render existing plate, if any
        // user chooses section
        // set volume on plate wells
        // user enters date, machine etc.
        // AJAX: add / remove reagents
        // AJAX: add / remove metadata
        // user clicks transfer button
        // POST to action bean
        // BettaLimsMessageResource processMessage or storeAndProcess

        // Prototype a JSP to render a JAXB DTO and rebuild it on POST
    }

}
