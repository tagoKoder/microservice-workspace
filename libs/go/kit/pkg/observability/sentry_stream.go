package observability

import "google.golang.org/grpc"

// stream wrapper para contar mensajes in/out
type SentryServerStream struct {
	grpc.ServerStream
	MsgIn, MsgOut int
}

func (s *SentryServerStream) RecvMsg(m any) error {
	err := s.ServerStream.RecvMsg(m)
	if err == nil {
		s.MsgIn++
	}
	return err
}
func (s *SentryServerStream) SendMsg(m any) error {
	err := s.ServerStream.SendMsg(m)
	if err == nil {
		s.MsgOut++
	}
	return err
}
