// bff\internal\client\grpc\conn.go
package grpc

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"os"

	"github.com/tagoKoder/bff/internal/config"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/credentials/insecure"
)

func dial(addr string, cfg config.Config) (*grpc.ClientConn, error) {
	opts, err := dialOptions(cfg)
	if err != nil {
		return nil, err
	}

	ctx := context.Background()
	//.WithTimeout(context.Background(), 5*time.Second)
	//defer cancel()

	opts = append(opts, grpc.WithBlock())
	return grpc.DialContext(ctx, addr, opts...)
}

func dialOptions(cfg config.Config) ([]grpc.DialOption, error) {
	if !cfg.GRPCUseTLS {
		return []grpc.DialOption{
			grpc.WithTransportCredentials(insecure.NewCredentials()),
			grpc.WithChainUnaryInterceptor(OutgoingHeadersUnaryInterceptor(), ClientMetadataInterceptor()),
			grpc.WithChainStreamInterceptor(OutgoingHeadersStreamInterceptor()),
		}, nil
	}

	caPem, err := os.ReadFile(cfg.GRPCCACertPath)
	if err != nil {
		return nil, fmt.Errorf("read CA: %w", err)
	}
	cp := x509.NewCertPool()
	if !cp.AppendCertsFromPEM(caPem) {
		return nil, fmt.Errorf("append CA failed")
	}

	tlsCfg := &tls.Config{
		RootCAs:    cp,
		MinVersion: tls.VersionTLS12,
	}

	if cfg.GRPCClientCertPath != "" && cfg.GRPCClientKeyPath != "" {
		cert, err := tls.LoadX509KeyPair(cfg.GRPCClientCertPath, cfg.GRPCClientKeyPath)
		if err != nil {
			return nil, fmt.Errorf("load client cert: %w", err)
		}
		tlsCfg.Certificates = []tls.Certificate{cert}
	}

	return []grpc.DialOption{
		grpc.WithTransportCredentials(credentials.NewTLS(tlsCfg)),
		grpc.WithChainUnaryInterceptor(OutgoingHeadersUnaryInterceptor(), ClientMetadataInterceptor()),
		grpc.WithChainStreamInterceptor(OutgoingHeadersStreamInterceptor()),
	}, nil
}
