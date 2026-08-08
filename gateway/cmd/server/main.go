package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/pploc/ms-gym-plans/gateway/internal/gateway"
)

func main() {
	httpAddr := envOr("HTTP_ADDR", ":8080")
	grpcAddr := envOr("PLANS_GRPC_ADDR", "127.0.0.1:50051")

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	handler, err := gateway.New(ctx, grpcAddr)
	if err != nil {
		log.Fatalf("gateway: %v", err)
	}
	server := gateway.ListenAndServe(httpAddr, handler)
	go func() {
		log.Printf("plans gateway listening on %s -> %s", httpAddr, grpcAddr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()

	<-ctx.Done()
	_ = server.Shutdown(context.Background())
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
