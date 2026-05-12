// Razorpay checkout integration. Uses test key by default.
// Override with VITE_RAZORPAY_KEY in your .env.
const RAZORPAY_KEY =
  (import.meta as any).env?.VITE_RAZORPAY_KEY ?? "rzp_test_1DP5mmOlF5G5ag";

let scriptPromise: Promise<boolean> | null = null;

export function loadRazorpay(): Promise<boolean> {
  if (typeof window === "undefined") return Promise.resolve(false);
  if ((window as any).Razorpay) return Promise.resolve(true);
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise((resolve) => {
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.onload = () => resolve(true);
    s.onerror = () => resolve(false);
    document.body.appendChild(s);
  });
  return scriptPromise;
}

export interface CheckoutOptions {
  amount: number; // in INR
  razorpayOrderId: string;
  name?: string;
  email?: string;
  description?: string;
  onSuccess: (resp: {
    razorpay_payment_id: string;
    razorpay_order_id: string;
    razorpay_signature: string;
  }) => void;
  onDismiss?: () => void;
}

export async function openCheckout(opts: CheckoutOptions) {
  const ok = await loadRazorpay();
  if (!ok) throw new Error("Failed to load Razorpay");
  const rzp = new (window as any).Razorpay({
    key: RAZORPAY_KEY,
    amount: Math.round(opts.amount * 100),
    currency: "INR",
    name: "FlowPay",
    description: opts.description ?? "Secure payment",
    order_id: opts.razorpayOrderId,
    prefill: {
      name: opts.name ?? "Demo User",
      email: opts.email ?? "demo@flowpay.io",
      contact: "9999999999",
    },
    notes: { source: "flowpay-web" },
    theme: { color: "#ef4444" },
    handler: (resp: any) => opts.onSuccess(resp),
    modal: { ondismiss: () => opts.onDismiss?.() },
  });
  rzp.open();
}
