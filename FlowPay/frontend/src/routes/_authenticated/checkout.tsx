import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { ordersApi } from "@/lib/api";
import { openCheckout } from "@/lib/razorpay";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import { PaymentSuccess } from "@/components/flowpay/PaymentSuccess";
import { CreditCard, Lock, Sparkles } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";

export const Route = createFileRoute("/_authenticated/checkout")({
  head: () => ({ meta: [{ title: "New payment — FlowPay" }] }),
  component: Checkout,
});

const presets = [499, 999, 1999, 4999];

function Checkout() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const nav = useNavigate();
  const [amount, setAmount] = useState<number>(999);
  const [busy, setBusy] = useState(false);
  const [success, setSuccess] = useState(false);

  const handlePay = async () => {
    if (!amount || amount < 1) return toast.error("Enter a valid amount");
    setBusy(true);
    try {
      // Step 1: Create order on our backend → get razorpayOrderId
      const order = await ordersApi.create(amount);

      // Step 2: Open Razorpay checkout with the order_id
      await openCheckout({
        amount,
        razorpayOrderId: order.razorpayOrderId,
        email: user?.email,
        description: `FlowPay order #${order.id}`,

        // Step 3: Razorpay calls this with snake_case fields after payment
        // Pass them directly to the backend — no key renaming needed
        onSuccess: async (resp) => {
          try {
            await ordersApi.verify({
              razorpay_order_id: resp.razorpay_order_id,
              razorpay_payment_id: resp.razorpay_payment_id,
              razorpay_signature: resp.razorpay_signature,
            });
            qc.invalidateQueries({ queryKey: ["orders"] });
            setSuccess(true);
          } catch (e: any) {
            toast.error(e.message || "Verification failed");
          }
        },

        onDismiss: () => toast.message("Payment cancelled"),
      });
    } catch (e: any) {
      toast.error(e.message || "Could not create order");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">New payment</h1>
        <p className="text-muted-foreground">
          Create a Razorpay order and complete payment in seconds.
        </p>
      </div>

      <div className="rounded-3xl bg-gradient-ink text-white p-8 shadow-card relative overflow-hidden">
        <div className="absolute -right-12 -top-12 h-48 w-48 rounded-full bg-gradient-primary opacity-30 blur-2xl" />
        <div className="flex items-center gap-2 text-white/70 text-sm">
          <Sparkles className="h-4 w-4" /> FlowPay Checkout
        </div>
        <div className="mt-2 text-5xl font-semibold tracking-tight">
          ₹{amount.toLocaleString("en-IN")}
        </div>
        <div className="mt-1 text-white/60 text-sm">{user?.email}</div>

        <div className="mt-8 grid sm:grid-cols-4 gap-3">
          {presets.map((p) => (
            <button
              key={p}
              onClick={() => setAmount(p)}
              className={`rounded-xl py-3 text-sm font-medium border transition ${
                amount === p
                  ? "bg-white text-foreground border-white"
                  : "bg-white/10 border-white/20 text-white/90 hover:bg-white/15"
              }`}
            >
              ₹{p.toLocaleString("en-IN")}
            </button>
          ))}
        </div>

        <label className="block mt-6 text-sm text-white/70">
          Custom amount (₹)
          <input
            type="number"
            min={1}
            value={amount}
            onChange={(e) => setAmount(Number(e.target.value))}
            className="mt-1.5 w-full rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-white outline-none focus:border-white"
          />
        </label>

        <button
          onClick={handlePay}
          disabled={busy}
          className="mt-6 w-full inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-primary text-white py-3.5 font-medium shadow-glow disabled:opacity-60"
        >
          <CreditCard className="h-4 w-4" />
          {busy ? "Opening Razorpay…" : `Pay ₹${amount.toLocaleString("en-IN")}`}
        </button>

        <div className="mt-4 flex items-center justify-center gap-1.5 text-xs text-white/50">
          <Lock className="h-3 w-3" /> Secured by Razorpay · Test mode
        </div>
      </div>

      {/* Test card details for Razorpay test mode */}
      <div className="rounded-2xl bg-secondary/60 p-5 text-sm text-muted-foreground space-y-1">
        <p className="font-semibold text-foreground mb-2">Test card details (Razorpay test mode)</p>
        <p><strong>Card number:</strong> 4111 1111 1111 1111</p>
        <p><strong>Expiry:</strong> Any future date (e.g. 12/26)</p>
        <p><strong>CVV:</strong> Any 3 digits (e.g. 123)</p>
        <p><strong>OTP:</strong> 1234</p>
        <p><strong>UPI (success):</strong> success@razorpay</p>
        <p><strong>UPI (failure):</strong> failure@razorpay</p>
      </div>

      <PaymentSuccess
        open={success}
        amount={amount}
        onClose={() => {
          setSuccess(false);
          nav({ to: "/dashboard" });
        }}
      />
    </div>
  );
}
