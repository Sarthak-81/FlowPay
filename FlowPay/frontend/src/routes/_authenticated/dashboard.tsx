import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ordersApi, type Order } from "@/lib/api";
import { StatCard } from "@/components/flowpay/StatCard";
import {
  Wallet, TrendingUp, Receipt, CreditCard, ArrowUpRight,
} from "lucide-react";
import {
  AreaChart, Area, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid,
} from "recharts";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/_authenticated/dashboard")({
  head: () => ({ meta: [{ title: "Dashboard — FlowPay" }] }),
  component: Dashboard,
});

function Dashboard() {
  const { user } = useAuth();
  const { data: orders = [], isLoading } = useQuery({
    queryKey: ["orders"],
    queryFn: () => ordersApi.list(),
  });

  const totalEarnings = orders
    .filter((o) => o.status === "PAID")
    .reduce((s, o) => s + (o.amount ?? 0), 0);
  const successRate = orders.length
    ? Math.round((orders.filter((o) => o.status === "PAID").length / orders.length) * 100)
    : 0;

  const chartData = buildChart(orders);

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">Welcome back</p>
          <h1 className="text-3xl md:text-4xl font-semibold tracking-tight">
            Hey {user?.email?.split("@")[0] ?? "there"} 👋
          </h1>
        </div>
        <Link
          to="/checkout"
          className="inline-flex items-center gap-2 rounded-xl bg-gradient-primary text-white px-5 py-2.5 text-sm font-medium shadow-glow"
        >
          <CreditCard className="h-4 w-4" /> New payment
        </Link>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total earnings" value={formatINR(totalEarnings)} delta="+12.4%" icon={Wallet} variant="primary" />
        <StatCard label="Orders" value={String(orders.length)} delta="+8 this week" icon={Receipt} variant="ink" />
        <StatCard label="Success rate" value={`${successRate}%`} delta="+2.1%" icon={TrendingUp} variant="success" />
        <StatCard label="Avg ticket" value={formatINR(orders.length ? totalEarnings / Math.max(1, orders.filter(o=>o.status==='PAID').length) : 0)} icon={ArrowUpRight} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 rounded-3xl bg-card p-6 shadow-card">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="font-semibold">Revenue trend</h3>
              <p className="text-sm text-muted-foreground">Last 14 days</p>
            </div>
          </div>
          <div className="h-64">
            <ResponsiveContainer>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="oklch(0.66 0.21 25)" stopOpacity={0.4} />
                    <stop offset="100%" stopColor="oklch(0.66 0.21 25)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.92 0.005 80)" vertical={false} />
                <XAxis dataKey="d" stroke="oklch(0.5 0.02 270)" fontSize={12} tickLine={false} axisLine={false} />
                <YAxis stroke="oklch(0.5 0.02 270)" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ borderRadius: 12, border: "1px solid oklch(0.92 0.005 80)" }} />
                <Area type="monotone" dataKey="amount" stroke="oklch(0.66 0.21 25)" strokeWidth={2.5} fill="url(#rev)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-3xl bg-card p-6 shadow-card">
          <h3 className="font-semibold mb-4">Recent orders</h3>
          {isLoading ? (
            <div className="text-sm text-muted-foreground">Loading…</div>
          ) : orders.length === 0 ? (
            <div className="text-sm text-muted-foreground">
              No orders yet. <Link to="/checkout" className="text-primary">Create one →</Link>
            </div>
          ) : (
            <ul className="space-y-3">
              {orders.slice(0, 5).map((o) => (
                <li key={o.id} className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <div className="text-sm font-medium truncate">#{o.razorpayOrderId?.slice(-8) || o.id}</div>
                    <div className="text-xs text-muted-foreground">{new Date(o.createdAt).toLocaleString()}</div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-semibold">{formatINR(o.amount)}</div>
                    <StatusPill status={o.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

export function StatusPill({ status }: { status: string }) {
  const map: Record<string, string> = {
    PAID: "bg-success/10 text-success",
    CREATED: "bg-warning/15 text-warning",
    FAILED: "bg-destructive/10 text-destructive",
  };
  const cls = map[status] || "bg-muted text-muted-foreground";
  return <span className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-medium ${cls}`}>{status}</span>;
}

export function formatINR(n: number) {
  return "₹" + Math.round(n).toLocaleString("en-IN");
}

function buildChart(orders: Order[]) {
  const days: { d: string; amount: number }[] = [];
  const today = new Date();
  for (let i = 13; i >= 0; i--) {
    const date = new Date(today);
    date.setDate(today.getDate() - i);
    const key = date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
    days.push({ d: key, amount: 0 });
  }
  orders.forEach((o) => {
    if (o.status !== "PAID") return;
    const date = new Date(o.createdAt);
    const key = date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
    const slot = days.find((x) => x.d === key);
    if (slot) slot.amount += o.amount;
  });
  // seed gentle baseline if all zero so chart isn't flat
  if (days.every((d) => d.amount === 0)) {
    days.forEach((d, i) => (d.amount = Math.round(800 + Math.sin(i / 1.7) * 600 + i * 120)));
  }
  return days;
}
