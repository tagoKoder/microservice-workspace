package middleware

import "strings"

func Contains(arr []string, s string) bool {
	for _, v := range arr {
		if strings.TrimSpace(v) == strings.TrimSpace(s) {
			return true
		}
	}
	return false
}
func JoinOrDefault(vals, def []string) string {
	if len(vals) == 0 {
		vals = def
	}
	for i := range vals {
		vals[i] = strings.TrimSpace(vals[i])
	}
	return strings.Join(vals, ",")
}
