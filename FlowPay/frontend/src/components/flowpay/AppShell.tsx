import { Link, Outlet, useLocation, useNavigate } from "@tanstack/react-router";
import { LayoutDashboard, Receipt, CreditCard, LogOut, Sparkles } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/dashboard", label: "Overview", icon: LayoutDashboard },
  { to: "/orders", label: "Orders", icon: Receipt },
  { to: "/checkout", label: "New Payment", icon: CreditCard },
];

export function AppShell() {
  const { user, logout } = useAuth();
  const loc = useLocation();
  const nav2 = useNavigate();

  return (
    <div className="min-h-screen flex bg-background">
      <aside className="hidden md:flex w-64 flex-col bg-gradient-ink text-sidebar-foreground p-5 gap-2">
        <div className="flex items-center gap-2 px-2 py-3 mb-4">
          <div className="h-9 w-9 rounded-xl bg-gradient-primary grid place-items-center shadow-glow">
            <Sparkles className="h-5 w-5 text-white" />
          </div>
          <div>
            <div className="text-base font-semibold tracking-tight">FlowPay</div>
            <div className="text-[11px] uppercase tracking-widest text-white/50">Fintech Suite</div>
          </div>
        </div>

        <nav className="flex flex-col gap-1">
          {nav.map((n) => {
            const active = loc.pathname.startsWith(n.to);
            return (
              <Link
                key={n.to}
                to={n.to}
                className={cn(
                  "flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all",
                  active
                    ? "bg-white/10 text-white shadow-soft"
                    : "text-white/65 hover:text-white hover:bg-white/5"
                )}
              >
                <n.icon className="h-4 w-4" />
                {n.label}
              </Link>
            );
          })}
        </nav>

        <div className="mt-auto rounded-2xl bg-white/5 p-4 border border-white/10">
          <div className="text-xs text-white/60">Signed in as</div>
          <div className="text-sm font-medium truncate">{user?.email ?? "Guest"}</div>
          <button
            onClick={() => { logout(); nav2({ to: "/login" }); }}
            className="mt-3 flex items-center gap-2 text-xs text-white/70 hover:text-white"
          >
            <LogOut className="h-3.5 w-3.5" /> Sign out
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="md:hidden flex items-center justify-between px-5 py-4 border-b">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-gradient-primary grid place-items-center">
              <Sparkles className="h-4 w-4 text-white" />
            </div>
            <span className="font-semibold">FlowPay</span>
          </div>
          <button onClick={() => { logout(); nav2({ to: "/login" }); }} className="text-sm text-muted-foreground">
            Sign out
          </button>
        </header>
        <main className="flex-1 p-6 md:p-10 max-w-[1400px] w-full mx-auto">
          <Outlet />
        </main>
        <nav className="md:hidden sticky bottom-0 bg-card border-t flex justify-around py-2">
          {nav.map((n) => {
            const active = loc.pathname.startsWith(n.to);
            return (
              <Link key={n.to} to={n.to}
                className={cn("flex flex-col items-center gap-0.5 px-4 py-1 text-xs",
                  active ? "text-primary" : "text-muted-foreground")}>
                <n.icon className="h-5 w-5" />
                {n.label}
              </Link>
            );
          })}
        </nav>
      </div>
    </div>
  );
}
