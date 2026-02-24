package com.tagokoder.account.infra.security.avp;

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

    public DecisionResult authorize(String accessToken,
                                String actionId,
                                String principalType,
                                String principalId,
                                Map<String, AttributeValue> principalAttrs,
                                String resourceType,
                                String resourceId,
                                Map<String, AttributeValue> resourceAttrs,
                                Map<String, AttributeValue> contextAttrs) {

        ActionIdentifier action = ActionIdentifier.builder()
                .actionType("ImaginaryBank::Action")
                .actionId(actionId)
                .build();

        EntityIdentifier principal = EntityIdentifier.builder()
                .entityType(principalType)              // "ImaginaryBank::User"
                .entityId(principalId)                  // "us-east-1_jpKpFUYH1|<sub>"
                .build();

        EntityIdentifier resource = EntityIdentifier.builder()
                .entityType(resourceType)               // "ImaginaryBank::Account"
                .entityId(resourceId)
                .build();

        EntityItem principalEntity = EntityItem.builder()
                .identifier(principal)
                .attributes(principalAttrs == null ? Map.of() : principalAttrs)
                .build();

        EntityItem resourceEntity = EntityItem.builder()
                .identifier(resource)
                .attributes(resourceAttrs == null ? Map.of() : resourceAttrs)
                .build();
                
        EntitiesDefinition entities = EntitiesDefinition.builder()
                .entityList(List.of(principalEntity, resourceEntity))
                .build();

        IsAuthorizedWithTokenRequest.Builder req = IsAuthorizedWithTokenRequest.builder()
                .policyStoreId(props.aws().avpPolicyStoreId())
                .accessToken(accessToken)
                .action(action)
                .resource(resource)
                .entities(entities);

        if (contextAttrs != null && !contextAttrs.isEmpty()) {
                req.context(ContextDefinition.fromContextMap(contextAttrs));
        }

        IsAuthorizedWithTokenResponse resp = avp.isAuthorizedWithToken(req.build());
        return new DecisionResult(resp.decision(), resp.determiningPolicies());
    }

    public record DecisionResult(Decision decision, List<DeterminingPolicyItem> determiningPolicies) {}
}
