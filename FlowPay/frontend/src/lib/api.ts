// Centralized API client for FlowPay backend.
// Base URL: set VITE_API_BASE_URL in .env (defaults to http://localhost:8080).
const BASE_URL =
  (import.meta as any).env?.VITE_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "flowpay_token";

export const tokenStore = {
  get: () => (typeof window === "undefined" ? null : localStorage.getItem(TOKEN_KEY)),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(
  path: string,
  options: RequestInit & { auth?: boolean; raw?: boolean } = {}
): Promise<T> {
  const { auth = true, raw = false, headers, ...rest } = options;
  const h: Record<string, string> = {
    "Content-Type": "application/json",
    ...(headers as Record<string, string> | undefined),
  };
  if (auth) {
    const t = tokenStore.get();
    if (t) h["Authorization"] = `Bearer ${t}`;
  }
  const res = await fetch(`${BASE_URL}${path}`, { ...rest, headers: h });
  const text = await res.text();
  if (!res.ok) {
    let msg = text || res.statusText;
    try {
      const j = JSON.parse(text);
      msg = j.message || j.error || msg;
    } catch {}
    throw new ApiError(msg, res.status);
  }
  if (raw) return text as unknown as T;
  if (!text) return undefined as unknown as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as unknown as T;
  }
}

// ---- Auth ----
export interface SignupPayload { name: string; email: string; password: string; }
export interface LoginPayload { email: string; password: string; }

export const authApi = {
  signup: (data: SignupPayload) =>
    request<{ message: string }>("/auth/signup", {
      method: "POST",
      body: JSON.stringify(data),
      auth: false,
    }),
  // backend returns the JWT as a plain string
  login: (data: LoginPayload) =>
    request<string>("/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
      auth: false,
      raw: true,
    }),
};

// ---- Orders ----
export interface Order {
  id: number;
  amount: number;
  status: string;
  userEmail: string;
  createdAt: string;
  razorpayOrderId: string;
}

export const ordersApi = {
  create: (amount: number) =>
    request<Order>("/api/orders", {
      method: "POST",
      body: JSON.stringify({ amount }),
    }),
  list: () => request<Order[]>("/api/orders", { method: "GET" }),
  verify: (data: {
    razorpayOrderId: string;
    razorpayPaymentId: string;
    razorpaySignature: string;
  }) =>
    request<string>("/api/orders/verify", {
      method: "POST",
      body: JSON.stringify(data),
      raw: true,
    }),
};
