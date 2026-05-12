import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { ArrowRight, Shield, Zap, BarChart3, CreditCard } from "lucide-react";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "FlowPay — Payments that flow" },
      { name: "description", content: "Production-grade payment infrastructure with Razorpay, real-time analytics and a beautiful dashboard." },
    ],
  }),
  component: Landing,
});

function Landing() {
  const { token, loading } = useAuth();
  const nav = useNavigate();
  useEffect(() => {
    if (!loading && token) nav({ to: "/dashboard" });
  }, [token, loading, nav]);

  return (
    <div className="min-h-screen bg-background">
      <header className="max-w-6xl mx-auto px-6 py-6 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="h-9 w-9 rounded-xl bg-gradient-primary shadow-glow" />
          <span className="font-semibold text-lg tracking-tight">FlowPay</span>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/login" className="text-sm text-muted-foreground hover:text-foreground">Sign in</Link>
          <Link to="/signup" className="text-sm rounded-xl bg-foreground text-background px-4 py-2 font-medium">
            Get started
          </Link>
        </div>
      </header>

      <section className="max-w-6xl mx-auto px-6 pt-20 pb-24 text-center">
        <span className="inline-flex items-center gap-2 rounded-full border bg-card px-3 py-1 text-xs font-medium text-muted-foreground shadow-soft">
          <span className="h-1.5 w-1.5 rounded-full bg-success" /> Live with Razorpay
        </span>
        <h1 className="mt-6 text-5xl md:text-7xl font-semibold tracking-tight leading-[1.05]">
          Payments that just <span className="text-gradient-primary">flow</span>.
        </h1>
        <p className="mt-6 max-w-2xl mx-auto text-lg text-muted-foreground">
          A production-grade fintech platform with secure JWT auth, real-time order tracking,
          and one-tap Razorpay checkout — all wrapped in a delightfully simple interface.
        </p>
        <div className="mt-9 flex justify-center gap-3">
          <Link to="/signup" className="inline-flex items-center gap-2 rounded-xl bg-gradient-primary text-white px-6 py-3 font-medium shadow-glow hover:opacity-95">
            Open dashboard <ArrowRight className="h-4 w-4" />
          </Link>
          <Link to="/login" className="rounded-xl border bg-card px-6 py-3 font-medium hover:bg-secondary">
            I have an account
          </Link>
        </div>

        <div className="mt-20 grid sm:grid-cols-2 lg:grid-cols-4 gap-4 text-left">
          {[
            { i: Zap, t: "Instant payouts", d: "Streamed via Kafka-ready services." },
            { i: Shield, t: "Bank-grade security", d: "JWT + Spring Security." },
            { i: BarChart3, t: "Live analytics", d: "Track revenue as it lands." },
            { i: CreditCard, t: "Razorpay native", d: "One-tap UPI, cards & wallets." },
          ].map((f) => (
            <div key={f.t} className="rounded-2xl bg-card p-6 shadow-card">
              <div className="h-10 w-10 rounded-xl bg-secondary text-primary grid place-items-center mb-3">
                <f.i className="h-5 w-5" />
              </div>
              <div className="font-semibold">{f.t}</div>
              <div className="text-sm text-muted-foreground mt-1">{f.d}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
