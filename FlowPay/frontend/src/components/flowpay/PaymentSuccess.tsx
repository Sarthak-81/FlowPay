import { motion, AnimatePresence } from "framer-motion";
import { Check } from "lucide-react";

interface Props { open: boolean; amount: number; onClose: () => void; }

export function PaymentSuccess({ open, amount, onClose }: Props) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 grid place-items-center bg-black/50 backdrop-blur-sm p-4"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            initial={{ scale: 0.85, y: 30, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            transition={{ type: "spring", damping: 18, stiffness: 220 }}
            className="bg-card rounded-3xl p-10 max-w-sm w-full text-center shadow-glow"
            onClick={(e) => e.stopPropagation()}
          >
            <motion.div
              className="mx-auto h-24 w-24 rounded-full bg-gradient-success grid place-items-center mb-6 relative"
              initial={{ scale: 0 }} animate={{ scale: 1 }}
              transition={{ delay: 0.1, type: "spring", damping: 12, stiffness: 200 }}
            >
              <motion.div
                className="absolute inset-0 rounded-full bg-success/40"
                initial={{ scale: 1, opacity: 0.6 }}
                animate={{ scale: 1.6, opacity: 0 }}
                transition={{ duration: 1.2, repeat: Infinity }}
              />
              <motion.div
                initial={{ pathLength: 0, opacity: 0 }}
                animate={{ pathLength: 1, opacity: 1 }}
                transition={{ delay: 0.35, duration: 0.4 }}
              >
                <Check className="h-12 w-12 text-white" strokeWidth={3} />
              </motion.div>
            </motion.div>
            <motion.h2
              className="text-2xl font-semibold tracking-tight"
              initial={{ y: 10, opacity: 0 }} animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.4 }}
            >
              Payment successful
            </motion.h2>
            <motion.p
              className="text-muted-foreground mt-2"
              initial={{ y: 10, opacity: 0 }} animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.5 }}
            >
              ₹{amount.toLocaleString("en-IN")} has been transferred securely.
            </motion.p>
            <motion.button
              onClick={onClose}
              className="mt-6 w-full rounded-xl bg-foreground text-background py-3 font-medium hover:opacity-90 transition"
              initial={{ y: 10, opacity: 0 }} animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.6 }}
            >
              Done
            </motion.button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
