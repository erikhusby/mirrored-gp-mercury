package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.datawh.EtlConfig;
import org.broadinstitute.gpinformatics.infrastructure.deckmsgs.DeckMessagesConfig;
import org.broadinstitute.gpinformatics.infrastructure.gap.GAPConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftConfig;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class ConfigProducer {

    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public QuoteConfig produceQuoteConfig() {
        return AbstractConfig.produce(QuoteConfig.class, deployment);
    }


    @Produces
    @Default
    public BSPConfig produceBSPConfig() {
        return AbstractConfig.produce(BSPConfig.class, deployment);
    }


    @Produces
    @Default
    public DeckMessagesConfig produceDeskMessagesConfig() {
        return AbstractConfig.produce(DeckMessagesConfig.class, deployment);
    }


    @Produces
    @Default
    public GAPConfig produceGAPConfig() {
        return AbstractConfig.produce(GAPConfig.class, deployment);
    }


    @Produces
    @Default
    public BettalimsConfig produceBettalimsConfig() {
        return AbstractConfig.produce(BettalimsConfig.class, deployment);
    }


    @Produces
    @Default
    public EtlConfig produceEtlConfig() {
        return AbstractConfig.produce(EtlConfig.class, deployment);
    }


    @Produces
    @Default
    public MercuryConfig produceMercuryConfig() {
        return AbstractConfig.produce(MercuryConfig.class, deployment);
    }


    @Produces
    @Default
    public TableauConfig produceTableauConfig() {
        return AbstractConfig.produce(TableauConfig.class, deployment);
    }


    @Produces
    @Default
    public SquidConfig produceSquidConfig() {
        return AbstractConfig.produce(SquidConfig.class, deployment);
    }


    @Produces
    @Default
    public JiraConfig produceJiraConfig() {
        return AbstractConfig.produce(JiraConfig.class, deployment);
    }


    @Produces
    @Default
    public ThriftConfig produceThriftConfig() {
        return AbstractConfig.produce(ThriftConfig.class, deployment);
    }

}
