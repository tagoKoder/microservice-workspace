package com.tagokoder.account.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
class PropsSanityCheck implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PropsSanityCheck.class);

  private final AppProps props;

  PropsSanityCheck(AppProps props) {
    this.props = requireNonNull(props);
  }

  @Override
  public void run(ApplicationArguments args) {
    // Validaciones (tal como lo tenías)
    requireNonBlank(props.security().issuerUri(), "COGNITO_ISSUER_URI / app.security.issuer-uri");
    requireNonBlank(props.security().audience(), "COGNITO_AUDIENCE / app.security.audience");
    requireNonBlank(props.aws().avpPolicyStoreId(), "AVP_POLICY_STORE_ID / app.aws.avpPolicyStoreId");
    requireNonBlank(props.aws().auditBusName(), "AUDIT_BUS_NAME / app.aws.auditBusName");

    // Logs (con enmascarado en caso de duda)
    log.info("==== App configuration (resumen) ====");
    log.info("security.issuer-uri: {}", safe(props.security().issuerUri(), false));  // normalmente OK público
    log.info("security.audience: {}", safe(props.security().audience(), false));     // si es sensible cámbialo a true
    log.info("aws.avpPolicyStoreId: {}", safe(props.aws().avpPolicyStoreId(), false));
    log.info("aws.auditBusName: {}", safe(props.aws().auditBusName(), false));
    log.info("====================================");
  }

  private static void requireNonBlank(String v, String name) {
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("Missing required config: " + name);
    }
  }

  /**
   * Enmascara el valor si se marca como sensible.
   * Ejemplo: "arn:aws:..." -> "arn:*********"
   */
  private static String safe(String value, boolean sensitive) {
    if (value == null) return "<null>";
    if (!sensitive) return value;
    if (value.length() <= 4) return "****";
    return value.substring(0, Math.min(4, value.length())) + "****";
  }
}