package messaging

type TemporaryPublishError struct {
	Message string
	Code    string
}

func (e *TemporaryPublishError) Error() string {
	if e.Code != "" {
		return e.Message + " (" + e.Code + ")"
	}
	return e.Message
}

// Útil si luego implementas retry con clasificación de error
func (e *TemporaryPublishError) Temporary() bool { return true }
