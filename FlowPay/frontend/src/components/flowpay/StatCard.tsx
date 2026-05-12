import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface Props {
  label: string;
  value: string;
  delta?: string;
  icon: LucideIcon;
  variant?: "primary" | "success" | "ink" | "plain";
}

export function StatCard({ label, value, delta, icon: Icon, variant = "plain" }: Props) {
  const styles = {
    primary: "bg-gradient-primary text-white shadow-glow",
    success: "bg-gradient-success text-white",
    ink: "bg-gradient-ink text-white",
    plain: "bg-card text-card-foreground shadow-card",
  }[variant];

  return (
    <div className={cn("rounded-3xl p-6 flex flex-col gap-4 relative overflow-hidden", styles)}>
      <div className="flex items-center justify-between">
        <span className={cn("text-sm font-medium opacity-80",
          variant === "plain" && "text-muted-foreground opacity-100")}>{label}</span>
        <div className={cn("h-9 w-9 rounded-xl grid place-items-center",
          variant === "plain" ? "bg-secondary text-primary" : "bg-white/15")}>
          <Icon className="h-4 w-4" />
        </div>
      </div>
      <div className="flex items-baseline gap-2">
        <span className="text-3xl font-semibold tracking-tight">{value}</span>
        {delta && (
          <span className={cn("text-xs font-medium",
            variant === "plain" ? "text-success" : "text-white/80")}>{delta}</span>
        )}
      </div>
    </div>
  );
}
