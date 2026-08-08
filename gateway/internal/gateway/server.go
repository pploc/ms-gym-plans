package gateway

import (
	"context"
	"net/http"
	"time"

	"github.com/grpc-ecosystem/grpc-gateway/v2/runtime"
	plansv1 "github.com/pploc/proto-go/v3/plans/v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/encoding/protojson"
)

func New(ctx context.Context, grpcAddr string) (http.Handler, error) {
	mux := runtime.NewServeMux(
		runtime.WithMarshalerOption(runtime.MIMEWildcard, &runtime.JSONPb{
			MarshalOptions:   protojson.MarshalOptions{UseProtoNames: true, EmitUnpopulated: false},
			UnmarshalOptions: protojson.UnmarshalOptions{DiscardUnknown: true},
		}),
		runtime.WithMetadata(incomingHeaders),
	)
	opts := []grpc.DialOption{grpc.WithTransportCredentials(insecure.NewCredentials())}
	if err := plansv1.RegisterPlansServiceHandlerFromEndpoint(ctx, mux, grpcAddr, opts); err != nil {
		return nil, err
	}
	root := http.NewServeMux()
	root.Handle("/healthz", http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	}))
	// ponytail: bare scrape target until OTel Prometheus exporter is wired in process.
	root.Handle("/metrics", http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "text/plain; version=0.0.4")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("# TYPE ms_gym_plans_gateway_up gauge\nms_gym_plans_gateway_up 1\n"))
	}))
	root.Handle("/", mux)
	return root, nil
}

func incomingHeaders(_ context.Context, r *http.Request) metadata.MD {
	md := metadata.MD{}
	for _, h := range []string{
		"x-user-id", "x-user-role", "x-gym-id", "x-membership-status",
		"traceparent", "tracestate", "x-trace-id",
	} {
		if v := r.Header.Get(h); v != "" {
			md.Set(h, v)
		}
	}
	return md
}

func ListenAndServe(addr string, handler http.Handler) *http.Server {
	return &http.Server{
		Addr:              addr,
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
	}
}
