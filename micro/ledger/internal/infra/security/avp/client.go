package avp

import (
	"context"
	"errors"

	"github.com/aws/aws-sdk-go-v2/service/verifiedpermissions"
	vptypes "github.com/aws/aws-sdk-go-v2/service/verifiedpermissions/types"
	"github.com/tagoKoder/ledger/internal/infra/security/authz"
)

type Decision string

const (
	DecisionAllow Decision = "ALLOW"
	DecisionDeny  Decision = "DENY"
)

type Client struct {
	sdk          *verifiedpermissions.Client
	policyStoreID string
}

func New(sdk *verifiedpermissions.Client, policyStoreID string) *Client {
	return &Client{sdk: sdk, policyStoreID: policyStoreID}
}

type Result struct {
	Decision Decision
	PolicyID string
}

func (c *Client) AuthorizeWithToken(
	ctx context.Context,
	accessToken string,
	action authz.Action,
	resource authz.Resource,
	contextMap map[string]vptypes.AttributeValue,
) (Result, error) {

	if c.policyStoreID == "" {
		return Result{}, errors.New("AVP_POLICY_STORE_ID is empty")
	}
	if accessToken == "" {
		return Result{}, errors.New("missing access token")
	}

	actType := "Action"
	resType := resource.Type
	if resType == "" {
		resType = "Unknown"
	}

	in := &verifiedpermissions.IsAuthorizedWithTokenInput{
		PolicyStoreId: &c.policyStoreID,
		AccessToken:   &accessToken,
		Action: &vptypes.ActionIdentifier{
			ActionType: actType,
			ActionId:   &action.ID,
		},
		Resource: &vptypes.EntityIdentifier{
			EntityType: &resType,
			EntityId:   &resource.ID,
		},
	}

	if contextMap != nil {
		in.Context = &vptypes.ContextDefinitionMemberContextMap{
			Value: contextMap,
		}
	}

	out, err := c.sdk.IsAuthorizedWithToken(ctx, in)
	if err != nil {
		return Result{}, err
	}

	r := Result{Decision: Decision(out.Decision)}
	if out.DeterminingPolicies != nil && len(out.DeterminingPolicies) > 0 {
		// Guardamos el primero como policy determinante (útil para auditoría).
		if out.DeterminingPolicies[0].PolicyId != nil {
			r.PolicyID = *out.DeterminingPolicies[0].PolicyId
		}
	}
	return r, nil
}
