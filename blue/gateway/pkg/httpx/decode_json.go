package httpx

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/tagoKoder/common-kit/pkg/datax"
)

// DecodeJSON parses a JSON request body into a struct of type <T>.
//
//	Parameters:
//		- <r>: <*http.Request> - the incoming HTTP request.
//
//	Returns:
//		- <*T>: pointer to the parsed struct.
//		- <error>: parsing error if any.
func DecodeJSONFromBody[T any](r *http.Request) (*T, error) {
	var result T
	err := json.NewDecoder(r.Body).Decode(&result)
	if err != nil {
		return nil, err
	}
	return &result, nil
}

// DecodeJSONGET extracts and parses multiple GET parameters from the URL query,
// matching each key to its expected type.
//
//	Parameters:
//		- <r>: <*http.Request> - the incoming HTTP request.
//		- <endpoint>: <string> - name of the endpoint, used in error messages.
//		- <keys>: <[]string> - list of query parameter keys to extract.
//		- <types>: <[]string> - expected types for each key.
//
//	Returns:
//		- <[]any>: list of parsed values in the same order as <keys>.
//		- <error>: error if parsing or validation fails.
func DecodeJSONGET(r *http.Request, endpoint string, keys []string, types []string) ([]any, error) {
	if len(keys) != len(types) {
		return nil, fmt.Errorf("keys and types must have the same length")
	}

	var valueList []any
	params := r.URL.Query()
	numParams := len(params)
	numKeys := len(keys)

	if numParams < numKeys {
		return nil, fmt.Errorf("index out of required params length: params (%d) - keys (%d)", numParams, numKeys)
	}

	for i, key := range keys {
		values := params[key] // Esto captura todos los valores para la clave `key`.
		if len(values) == 0 {
			return nil, fmt.Errorf("%s is required on endpoint: %s", key, endpoint)
		}

		// Procesa cada valor dependiendo del tipo.
		var parsedValues []any
		for _, value := range values {
			v, err := datax.ParseValueByType(types[i], value)
			if err != nil {
				return nil, fmt.Errorf("%s for key: %s", err.Error(), key)
			}
			parsedValues = append(parsedValues, v)
		}

		// Si solo hay un valor y no es una lista, devuelve directamente.
		if len(parsedValues) == 1 {
			valueList = append(valueList, parsedValues[0])
		} else {
			// Si hay múltiples valores, los empaqueta como un slice.
			valueList = append(valueList, parsedValues)
		}
	}

	return valueList, nil
}
