// Razorpay checkout integration.
// The key MUST match the one used on the backend (application.yaml razorpay.key).
// Set VITE_RAZORPAY_KEY in your .env to override.
const RAZORPAY_KEY =
  (import.meta as any).env?.VITE_RAZORPAY_KEY ?? "rzp_test_SjJbldo92o9fp3";

let scriptPromise: Promise<boolean> | null = null;

/**
 * Dynamically loads the Razorpay checkout script.
 * Safe to call multiple times — subsequent calls reuse the same Promise.
 */
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
  amount: number;           // in INR (rupees), NOT paise
  razorpayOrderId: string;  // order_xxx from your backend
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

/**
 * Opens the Razorpay checkout modal.
 *
 * Key points:
 * - The `key` here MUST be the same test/live key used when the backend
 *   created the Razorpay order. A mismatch causes the "Uh oh!" error.
 * - `amount` is converted from rupees to paise (×100) here.
 * - `order_id` links this checkout session to the Razorpay order.
 * - The `handler` callback receives snake_case fields directly from Razorpay
 *   which are forwarded as-is to the backend verify endpoint.
 */
export async function openCheckout(opts: CheckoutOptions) {
  const ok = await loadRazorpay();
  if (!ok) throw new Error("Failed to load Razorpay checkout script");

  const rzp = new (window as any).Razorpay({
    key: RAZORPAY_KEY,
    amount: Math.round(opts.amount * 100), // rupees → paise
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
    // Razorpay calls handler with { razorpay_payment_id, razorpay_order_id, razorpay_signature }
    handler: (resp: any) => opts.onSuccess(resp),
    modal: { ondismiss: () => opts.onDismiss?.() },
  });

  rzp.open();
}
