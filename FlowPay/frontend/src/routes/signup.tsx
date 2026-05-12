import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import { AuthLayout } from "./login";

export const Route = createFileRoute("/signup")({
  head: () => ({ meta: [{ title: "Create account — FlowPay" }] }),
  component: SignupPage,
});

function SignupPage() {
  const { signup } = useAuth();
  const nav = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await signup({ name, email, password });
      toast.success("Account created");
      nav({ to: "/dashboard" });
    } catch (err: any) {
      toast.error(err.message || "Signup failed");
    } finally { setBusy(false); }
  };

  return (
    <AuthLayout title="Create your account" subtitle="Start accepting payments in minutes">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Full name" type="text" value={name} onChange={setName} />
        <Field label="Email" type="email" value={email} onChange={setEmail} />
        <Field label="Password" type="password" value={password} onChange={setPassword} />
        <button disabled={busy} className="w-full rounded-xl bg-gradient-primary text-white py-3 font-medium shadow-glow disabled:opacity-60">
          {busy ? "Creating…" : "Create account"}
        </button>
      </form>
      <p className="text-sm text-center mt-6 text-muted-foreground">
        Already have one? <Link to="/login" className="text-primary font-medium">Sign in</Link>
      </p>
    </AuthLayout>
  );
}

function Field({ label, type, value, onChange }: any) {
  return (
    <label className="block">
      <span className="text-sm font-medium">{label}</span>
      <input
        type={type} required value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1.5 w-full rounded-xl border bg-card px-4 py-3 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition"
      />
    </label>
  );
}
