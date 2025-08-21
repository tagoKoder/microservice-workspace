package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	gw "github.com/tagoKoder/gateway/internal/entrypoint/http"
)

func main() {
	ctx := context.Background()

	srv, err := gw.NewServer(ctx)
	if err != nil {
		log.Fatal(err)
	}
	log.Println("gateway HTTP server started")
	// Run
	go func() {
		log.Println("gateway HTTP listening on", os.Getenv("HTTP_ADDR"))
		if err := srv.Start(); err != nil {
			log.Println("server stopped:", err)
		}
	}()

	// Graceful shutdown
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGINT, syscall.SIGTERM)
	<-sig

	shctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shctx)
}
