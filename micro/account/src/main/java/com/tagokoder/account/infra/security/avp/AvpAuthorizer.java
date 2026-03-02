package com.tagokoder.account.infra.security.avp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tagokoder.account.infra.config.AppProps;

import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;
import software.amazon.awssdk.services.verifiedpermissions.model.ActionIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.ContextDefinition;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;
import software.amazon.awssdk.services.verifiedpermissions.model.DeterminingPolicyItem;
import software.amazon.awssdk.services.verifiedpermissions.model.EntitiesDefinition;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityItem;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenResponse;

public class AvpAuthorizer {

  private final VerifiedPermissionsClient avp;
  private final AppProps props;

  public AvpAuthorizer(VerifiedPermissionsClient avp, AppProps props) {
    this.avp = avp;
    this.props = props;
  }

  public DecisionResult authorizeWithToken(
      String accessToken,
      String actionId,
      EntityItem principalEntity,
      EntityItem resourceEntity,
      List<EntityItem> extraEntities,
      Map<String, AttributeValue> contextAttrs
  ) {
    ActionIdentifier action = ActionIdentifier.builder()
        .actionType("ImaginaryBank::Action")
        .actionId(actionId)
        .build();

    EntityIdentifier resourceId = resourceEntity.identifier();

    List<EntityItem> all = new ArrayList<>();
    //all.add(principalEntity);
    all.add(resourceEntity);
    if (extraEntities != null && !extraEntities.isEmpty()) all.addAll(extraEntities);

    EntitiesDefinition entities = EntitiesDefinition.builder()
        .entityList(all)
        .build();

    IsAuthorizedWithTokenRequest.Builder req = IsAuthorizedWithTokenRequest.builder()
        .policyStoreId(props.aws().avpPolicyStoreId())
        .accessToken(accessToken)
        .action(action)
        .resource(resourceId)
        .entities(entities);

    if (contextAttrs != null && !contextAttrs.isEmpty()) {
      req.context(ContextDefinition.fromContextMap(contextAttrs));
    }

    IsAuthorizedWithTokenResponse resp = avp.isAuthorizedWithToken(req.build());
    return new DecisionResult(resp.decision(), resp.determiningPolicies());
  }

  public record DecisionResult(Decision decision, List<DeterminingPolicyItem> determiningPolicies) {}
}