// micro\ledger\internal\infra\security\authz\resource_resolver_reflect.go
package authz

import (
	"reflect"

	"github.com/google/uuid"
)

func ExtractStringField(obj any, field string) string {
	if obj == nil {
		return ""
	}
	v := reflect.ValueOf(obj)
	if v.Kind() == reflect.Pointer {
		v = v.Elem()
	}
	if !v.IsValid() || v.Kind() != reflect.Struct {
		return ""
	}
	f := v.FieldByName(field)
	if !f.IsValid() {
		return ""
	}
	if f.Kind() == reflect.String {
		return f.String()
	}
	return ""
}

func extractUUIDStringField(obj any, field string) string {
	if obj == nil {
		return ""
	}
	v := reflect.ValueOf(obj)
	if v.Kind() == reflect.Pointer {
		v = v.Elem()
	}
	if !v.IsValid() || v.Kind() != reflect.Struct {
		return ""
	}
	f := v.FieldByName(field)
	if !f.IsValid() {
		return ""
	}
	// uuid.UUID
	if f.Type() == reflect.TypeOf(uuid.UUID{}) {
		u := f.Interface().(uuid.UUID)
		if u == uuid.Nil {
			return ""
		}
		return u.String()
	}
	return ""
}
