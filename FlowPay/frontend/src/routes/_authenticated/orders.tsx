import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ordersApi } from "@/lib/api";
import { formatINR, StatusPill } from "./dashboard";
import { Plus } from "lucide-react";

export const Route = createFileRoute("/_authenticated/orders")({
  head: () => ({ meta: [{ title: "Orders — FlowPay" }] }),
  component: OrdersPage,
});

function OrdersPage() {
  const { data: orders = [], isLoading } = useQuery({
    queryKey: ["orders"],
    queryFn: () => ordersApi.list(),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Orders</h1>
          <p className="text-muted-foreground">All payment orders for your account</p>
        </div>
        <Link to="/checkout" className="inline-flex items-center gap-2 rounded-xl bg-foreground text-background px-4 py-2 text-sm font-medium">
          <Plus className="h-4 w-4" /> New
        </Link>
      </div>

      <div className="rounded-3xl bg-card shadow-card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-secondary text-muted-foreground text-xs uppercase tracking-wider">
            <tr>
              <th className="text-left font-medium px-6 py-3">Order</th>
              <th className="text-left font-medium px-6 py-3">Razorpay ID</th>
              <th className="text-left font-medium px-6 py-3">Created</th>
              <th className="text-right font-medium px-6 py-3">Amount</th>
              <th className="text-right font-medium px-6 py-3">Status</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={5} className="px-6 py-10 text-center text-muted-foreground">Loading…</td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan={5} className="px-6 py-12 text-center text-muted-foreground">No orders yet.</td></tr>
            ) : orders.map((o) => (
              <tr key={o.id} className="border-t hover:bg-secondary/40">
                <td className="px-6 py-4 font-medium">#{o.id}</td>
                <td className="px-6 py-4 font-mono text-xs text-muted-foreground">{o.razorpayOrderId}</td>
                <td className="px-6 py-4 text-muted-foreground">{new Date(o.createdAt).toLocaleString()}</td>
                <td className="px-6 py-4 text-right font-semibold">{formatINR(o.amount)}</td>
                <td className="px-6 py-4 text-right"><StatusPill status={o.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
