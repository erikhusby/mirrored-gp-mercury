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
        return QuoteConfig.produce(deployment);
    }


    @Produces
    @Default
    public BSPConfig produceBSPConfig() {
        return BSPConfig.produce(deployment);
    }


    @Produces
    @Default
    public DeckMessagesConfig produceDeskMessagesConfig() {
        return DeckMessagesConfig.produce(deployment);
    }


    @Produces
    @Default
    public GAPConfig produceGAPConfig() {
        return GAPConfig.produce(deployment);
    }


    @Produces
    @Default
    public BettalimsConfig produceBettalimsConfig() {
        return BettalimsConfig.produce(deployment);
    }


    @Produces
    @Default
    public EtlConfig produceEtlConfig() {
        return EtlConfig.produce(deployment);
    }


    @Produces
    @Default
    public MercuryConfig produceMercuryConfig() {
        return MercuryConfig.produce(deployment);
    }


    @Produces
    @Default
    public TableauConfig produceTableauConfig() {
        return TableauConfig.produce(deployment);
    }


    @Produces
    @Default
    public SquidConfig produceSquidConfig() {
        return SquidConfig.produce(deployment);
    }


    @Produces
    @Default
    public JiraConfig produceJiraConfig() {
        return JiraConfig.produce(deployment);
    }


    @Produces
    @Default
    public ThriftConfig produceThriftConfig() {
        return ThriftConfig.produce(deployment);
    }

}
