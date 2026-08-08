package gateway

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestGivenHealthEndpoint_whenGet_thenReturnsOk(t *testing.T) {
	// Given
	// Dial failure is deferred until first RPC; health handler is local.
	handler, err := New(context.Background(), "127.0.0.1:1")
	if err != nil {
		t.Fatalf("new gateway: %v", err)
	}

	// When
	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)
	rr := httptest.NewRecorder()
	handler.ServeHTTP(rr, req)

	// Then
	if rr.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rr.Code)
	}
	if body := rr.Body.String(); body != "ok" {
		t.Fatalf("body = %q, want ok", body)
	}
}

func TestGivenTrustedHeaders_whenIncomingHeaders_thenForwardsOnlyAllowlist(t *testing.T) {
	// Given
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("x-user-id", "u1")
	req.Header.Set("x-user-role", "ADMIN")
	req.Header.Set("x-gym-id", "g1")
	req.Header.Set("authorization", "Bearer secret")
	req.Header.Set("traceparent", "00-trace")

	// When
	md := incomingHeaders(context.Background(), req)

	// Then
	if got := md.Get("x-user-id"); len(got) != 1 || got[0] != "u1" {
		t.Fatalf("x-user-id = %v", got)
	}
	if got := md.Get("authorization"); len(got) != 0 {
		t.Fatalf("authorization must not forward: %v", got)
	}
	if got := md.Get("traceparent"); len(got) != 1 || got[0] != "00-trace" {
		t.Fatalf("traceparent = %v", got)
	}
}
