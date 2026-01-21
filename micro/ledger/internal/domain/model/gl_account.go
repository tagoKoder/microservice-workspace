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
