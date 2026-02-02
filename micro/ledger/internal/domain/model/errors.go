package model

import "errors"

var ErrAmountNotExact = errors.New("amount cannot be represented exactly as float64")
