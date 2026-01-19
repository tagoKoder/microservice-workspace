package com.tagokoder.identity.infra.security.avp;

import com.tagokoder.identity.infra.config.AppProps;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;
import software.amazon.awssdk.services.verifiedpermissions.model.*;

import java.util.List;
import java.util.Map;

public class AvpAuthorizer {

    private final VerifiedPermissionsClient avp;
    private final AppProps props;

    public AvpAuthorizer(VerifiedPermissionsClient avp, AppProps props) {
        this.avp = avp;
        this.props = props;
    }

    public DecisionResult authorize(String accessToken,
                                    String actionId,
                                    String resourceType,
                                    String resourceId,
                                    Map<String, AttributeValue> resourceAttrs,
                                    Map<String, AttributeValue> contextAttrs) {

        ActionIdentifier action = ActionIdentifier.builder()
                .actionType("Action")
                .actionId(actionId)
                .build();

        EntityIdentifier resource = EntityIdentifier.builder()
                .entityType(resourceType)
                .entityId(resourceId)
                .build();

        EntityItem resourceEntity = EntityItem.builder()
                .identifier(resource)
                .attributes(resourceAttrs == null ? Map.of() : resourceAttrs)
                .build();

        EntitiesDefinition entities = EntitiesDefinition.builder()
                .entityList(List.of(resourceEntity))
                .build();

        IsAuthorizedWithTokenRequest.Builder req = IsAuthorizedWithTokenRequest.builder()
                .policyStoreId(props.aws().avpPolicyStoreId())
                .accessToken(accessToken)
                .action(action)
                .resource(resource)
                .entities(entities);

        if (contextAttrs != null && !contextAttrs.isEmpty()) {
            req.context(ContextDefinition.builder().contextMap(contextAttrs).build());
        }

        IsAuthorizedWithTokenResponse resp = avp.isAuthorizedWithToken(req.build());

        return new DecisionResult(resp.decision(), resp.determiningPolicies());
    }

    public record DecisionResult(Decision decision, List<DeterminingPolicyItem> determiningPolicies) {}
}
