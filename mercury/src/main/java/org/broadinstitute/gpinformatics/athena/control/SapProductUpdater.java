/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.control;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import java.util.List;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Registry of scheduled items for the application.  To use this class, subclass AbstractCache
 * and override refreshCache().
 */
@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class SapProductUpdater {

    private static final Log logger = LogFactory.getLog(SapProductUpdater.class);

    private final SessionContextUtility sessionContextUtility;
    private final ProductDao productDao;
    private final ProductEjb productEjb;
    private final Log log = LogFactory.getLog(SapProductUpdater.class);

    @Inject
    public SapProductUpdater(SessionContextUtility sessionContextUtility, ProductDao productDao,
                             ProductEjb productEjb) {
        this.sessionContextUtility = sessionContextUtility;
        this.productDao = productDao;
        this.productEjb = productEjb;
    }

    public SapProductUpdater() {
        this(null, null, null);
    }

    /**
     * The schedule is daily 2 minutes after 4:00. It is offset
     * by 2 minutes because the Quote server reboots at midnight (prod) and 3AM (dev).
     */
    @Schedule(minute = "2", dayOfWeek = "*", hour = "4", persistent = false)
    public void updateProductsInSap() {
        sessionContextUtility.executeInContext(() -> {
            List<Product> products = productDao
                .findProducts(ProductDao.Availability.EXPIRED, ProductDao.TopLevelOnly.NO, ProductDao.IncludePDMOnly.NO);
            try {
                productEjb.publishProductsToSAP(products);
            } catch (ValidationException e) {
                log.error("Could nodt update product(s) in SAP", e);
            }
        });
    }
}
