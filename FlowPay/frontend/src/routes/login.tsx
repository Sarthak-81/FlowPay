import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import { Sparkles } from "lucide-react";

export const Route = createFileRoute("/login")({
  head: () => ({ meta: [{ title: "Sign in — FlowPay" }] }),
  component: LoginPage,
});

function LoginPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await login({ email, password });
      toast.success("Welcome back!");
      nav({ to: "/dashboard" });
    } catch (err: any) {
      toast.error(err.message || "Login failed");
    } finally { setBusy(false); }
  };

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to your FlowPay account">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email" type="email" value={email} onChange={setEmail} />
        <Field label="Password" type="password" value={password} onChange={setPassword} />
        <button disabled={busy} className="w-full rounded-xl bg-gradient-primary text-white py-3 font-medium shadow-glow disabled:opacity-60">
          {busy ? "Signing in…" : "Sign in"}
        </button>
      </form>
      <p className="text-sm text-center mt-6 text-muted-foreground">
        New here? <Link to="/signup" className="text-primary font-medium">Create account</Link>
      </p>
    </AuthLayout>
  );
}

export function AuthLayout({ title, subtitle, children }: any) {
  return (
    <div className="min-h-screen grid lg:grid-cols-2">
      <div className="hidden lg:flex bg-gradient-ink text-white p-12 flex-col justify-between">
        <div className="flex items-center gap-2">
          <div className="h-9 w-9 rounded-xl bg-gradient-primary grid place-items-center shadow-glow">
            <Sparkles className="h-5 w-5" />
          </div>
          <span className="font-semibold tracking-tight">FlowPay</span>
        </div>
        <div>
          <h2 className="text-4xl font-semibold tracking-tight leading-tight">
            The cleanest way to <span className="text-gradient-primary">accept payments</span>.
          </h2>
          <p className="mt-4 text-white/60 max-w-md">
            FlowPay handles the heavy lifting — auth, orders, Razorpay & reconciliation —
            so you can focus on growing your business.
          </p>
        </div>
        <div className="text-xs text-white/40">© FlowPay · Secured by Spring & JWT</div>
      </div>
      <div className="flex items-center justify-center p-6 md:p-12 bg-background">
        <div className="w-full max-w-sm">
          <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
          <p className="text-muted-foreground mt-1">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, type, value, onChange }: any) {
  return (
    <label className="block">
      <span className="text-sm font-medium">{label}</span>
      <input
        type={type}
        required
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1.5 w-full rounded-xl border bg-card px-4 py-3 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition"
      />
    </label>
  );
}
