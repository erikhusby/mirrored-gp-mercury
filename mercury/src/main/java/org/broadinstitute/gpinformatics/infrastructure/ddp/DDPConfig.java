package org.broadinstitute.gpinformatics.infrastructure.ddp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.TokenGenerator;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Connection information for the DDP web resources.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("ddp")
@ApplicationScoped
public class DDPConfig extends AbstractConfig implements Serializable, TokenGenerator {
    private Logger log = Logger.getLogger(DDPConfig.class);

    private String host;

    private String jwtSecret;

    private String jwtSecretFilename;

    public DDPConfig() {
    }

    @Inject
    public DDPConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getJwtSecretFilename() {
        return jwtSecretFilename;
    }

    public void setJwtSecretFilename(String jwtSecretFilename) {
        this.jwtSecretFilename = jwtSecretFilename;
    }

    public String getJwtSecret() {
        if (jwtSecret == null && jwtSecretFilename != null) {
            File homeDir = new File(System.getProperty("user.home"));
            File jwtSecretFile = new File(homeDir, jwtSecretFilename);
            try {
                if (jwtSecretFile.exists()) {
                    jwtSecret = FileUtils.readFileToString(jwtSecretFile).trim();
                } else {
                    String errMsg = "jwt secret file not found: " + jwtSecretFile.getPath();
                    log.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
            } catch (IOException e) {
                log.error("Failed to read jwt secret file: " + jwtSecretFile.getPath(), e);
                throw new RuntimeException(e.getMessage());
            }
        }

        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getUrl(String suffix) {
        return String.format("https://%s/ddp/%s", getHost(), suffix);
    }

    public static DDPConfig produce(Deployment deployment) {
        return produce(DDPConfig.class, deployment);
    }

    public String generateToken() {
        Instant expiration = LocalDateTime.now()
                .plusMinutes(5)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());

        Date expirDate = Date.from(expiration);

        // JW: My mac is 10 seconds ahead, not sure how this'll affect produciton
        Instant iat = LocalDateTime.now()
                .minusSeconds(60)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        Date iatDate = Date.from(iat);

        return JWT.create()
                .withIssuer(getUrl(""))
                .withExpiresAt(expirDate)
                .withIssuedAt(iatDate)
                .sign(algorithm);
    }
}
