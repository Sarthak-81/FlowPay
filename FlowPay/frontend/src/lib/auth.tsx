import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { authApi, tokenStore, type LoginPayload, type SignupPayload } from "./api";

interface DecodedUser { email: string; name?: string; }

interface AuthCtx {
  user: DecodedUser | null;
  token: string | null;
  loading: boolean;
  login: (p: LoginPayload) => Promise<void>;
  signup: (p: SignupPayload) => Promise<void>;
  logout: () => void;
}

const Ctx = createContext<AuthCtx | null>(null);

function decode(token: string): DecodedUser | null {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return { email: payload.sub ?? payload.email ?? "user" };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<DecodedUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const t = tokenStore.get();
    if (t) {
      setToken(t);
      setUser(decode(t));
    }
    setLoading(false);
  }, []);

  const login = async (p: LoginPayload) => {
    const t = await authApi.login(p);
    const clean = t.replace(/^"|"$/g, "").trim();
    tokenStore.set(clean);
    setToken(clean);
    setUser(decode(clean) ?? { email: p.email });
  };

  const signup = async (p: SignupPayload) => {
    await authApi.signup(p);
    await login({ email: p.email, password: p.password });
  };

  const logout = () => {
    tokenStore.clear();
    setToken(null);
    setUser(null);
  };

  return (
    <Ctx.Provider value={{ user, token, loading, login, signup, logout }}>
      {children}
    </Ctx.Provider>
  );
}

export function useAuth() {
  const v = useContext(Ctx);
  if (!v) throw new Error("useAuth must be used inside AuthProvider");
  return v;
}
