import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth";
import { AppShell } from "@/components/flowpay/AppShell";

export const Route = createFileRoute("/_authenticated")({
  component: AuthGuard,
});

function AuthGuard() {
  const { token, loading } = useAuth();
  const nav = useNavigate();

  useEffect(() => {
    if (!loading && !token) nav({ to: "/login" });
  }, [token, loading, nav]);

  if (loading || !token) {
    return (
      <div className="min-h-screen grid place-items-center bg-background">
        <div className="h-10 w-10 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  return <AppShell />;
}
