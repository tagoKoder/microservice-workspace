package datax

import (
	"fmt"
	"strconv"
)

type DataType = string

const (
	INT32   DataType = "int32"
	INT64   DataType = "int64"
	INT     DataType = "int"
	BOOL    DataType = "bool"
	FLOAT32 DataType = "float32"
	FLOAT64 DataType = "float64"
	FLOAT   DataType = "float"
	STRING  DataType = "string"
)

func ParseValueByType(vType DataType, value string) (any, error) {
	switch vType {
	case "int32":
		intValue, err := strconv.Atoi(value)
		if err != nil {
			return nil, fmt.Errorf("invalid int value [%v]", err)
		}
		return int32(intValue), nil
	case "int64":
		intValue, err := strconv.Atoi(value)
		if err != nil {
			return nil, fmt.Errorf("invalid int value [%v]", err)
		}
		return int64(intValue), nil
	case "int":
		intValue, err := strconv.Atoi(value)
		if err != nil {
			return nil, fmt.Errorf("invalid int value [%v]", err)
		}
		return intValue, nil
	case "bool":
		boolValue, err := strconv.ParseBool(value)
		if err != nil {
			return nil, fmt.Errorf("invalid bool value for key [%v]", err)
		}
		return boolValue, nil
	case "float32":
		floatValue, err := strconv.ParseFloat(value, 64)
		if err != nil {
			return nil, fmt.Errorf("invalid float value for key [%v]", err)
		}
		return float32(floatValue), nil
	case "float64":
		floatValue, err := strconv.ParseFloat(value, 64)
		if err != nil {
			return nil, fmt.Errorf("invalid float value for key [%v]", err)
		}
		return float64(floatValue), nil
	case "float":
		floatValue, err := strconv.ParseFloat(value, 64)
		if err != nil {
			return nil, fmt.Errorf("invalid float value for key [%v]", err)
		}
		return floatValue, nil
	case "string":
		return value, nil
	default:
		return nil, fmt.Errorf("unsupported type %s", vType)
	}
}
