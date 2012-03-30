import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author breilly
 */
public class GreeterTest extends Arquillian {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
//                .addClasses(Greeter.class, PhraseBuilder.class)
                .addPackages(true, "")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Inject
    Greeter greeter;

    @Test
    public void should_create_greeting() {
        Assert.assertEquals("Hello, Arquillian!",
                greeter.createGreeting("Arquillian"));
        greeter.greet(System.out, "Arquillian");
    }
}
