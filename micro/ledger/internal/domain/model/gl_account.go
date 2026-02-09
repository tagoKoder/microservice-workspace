// micro\ledger\internal\domain\model\gl_account.go
package model

import "github.com/google/uuid"

type GLAccountType string

const (
	GLAsset     GLAccountType = "asset"
	GLLiability GLAccountType = "liability"
	GLIncome    GLAccountType = "income"
	GLExpense   GLAccountType = "expense"
	GLEquity    GLAccountType = "equity"
)

type GLAccount struct {
	ID   uuid.UUID
	Code string
	Name string
	Type GLAccountType
}

var (
	GLSystemFundID   = uuid.MustParse("11111111-1111-1111-1111-111111111111")
	GLCustomerCashID = uuid.MustParse("22222222-2222-2222-2222-222222222222")
	GLOutID          = uuid.MustParse("33333333-3333-3333-3333-333333333333")
	GLInID           = uuid.MustParse("44444444-4444-4444-4444-444444444444")
)
