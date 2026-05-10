import { useState, useEffect, useCallback, useRef } from 'react';
import API from './api';

// ─── Icons ────────────────────────────────────────────────────────────────────
const Icon = ({ d, size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d={d} />
  </svg>
);

const icons = {
  logout:  "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9",
  orders:  "M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2",
  pay:     "M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 0 0 3-3V8a3 3 0 0 0-3-3H6a3 3 0 0 0-3 3v8a3 3 0 0 0 3 3z",
  refresh: "M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15",
  user:    "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8",
};

// ─── Helpers ──────────────────────────────────────────────────────────────────
const formatCurrency = (n) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n);

const formatDate = (iso) => {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

const statusColor = (s) => ({
  CREATED: '#f59e0b',
  PAID:    '#10b981',
  FAILED:  '#ef4444',
}[s] ?? '#94a3b8');

// ─── Toast ────────────────────────────────────────────────────────────────────
function Toast({ toasts }) {
  return (
    <div style={{ position: 'fixed', top: 24, right: 24, zIndex: 9999, display: 'flex', flexDirection: 'column', gap: 10 }}>
      {toasts.map(t => (
        <div key={t.id} style={{
          background: t.type === 'error' ? 'rgba(239,68,68,0.15)' : 'rgba(16,185,129,0.15)',
          border: `1px solid ${t.type === 'error' ? '#ef4444' : '#10b981'}`,
          color: '#f1f5f9', padding: '12px 18px', borderRadius: 12,
          backdropFilter: 'blur(12px)', fontSize: 14, maxWidth: 320,
          animation: 'fadeSlide 0.3s ease',
        }}>
          {t.msg}
        </div>
      ))}
    </div>
  );
}

// ─── Auth Panel ───────────────────────────────────────────────────────────────
function AuthPanel({ onLogin, pushToast }) {
  const [mode, setMode]         = useState('login');
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading]   = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) { pushToast('Please fill in all fields', 'error'); return; }
    setLoading(true);
    try {
      if (mode === 'signup') {
        await API.post('/auth/signup', { email, password });
        pushToast('Account created! Please log in.', 'success');
        setMode('login');
      } else {
        const { data } = await API.post('/auth/login', { email, password });
        // Backend returns raw JWT string, not { token: "..." }
        const token = typeof data === 'object' ? data.token : data;
        localStorage.setItem('token', token);
        pushToast('Welcome back!', 'success');
        onLogin(email);
      }
    } catch (err) {
      const raw = err.response?.data;
      const msg = (typeof raw === 'string' ? raw : raw?.message)
        || (mode === 'login' ? 'Login failed. Check your credentials.' : 'Signup failed.');
      pushToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card-glass" style={{ maxWidth: 420, width: '100%' }}>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div className="logo">⚡ FlowPay</div>
        <p style={{ color: '#94a3b8', marginTop: 6, fontSize: 14 }}>
          Secure payments powered by Razorpay
        </p>
      </div>

      <div className="tab-row">
        <button className={`tab-btn${mode === 'login' ? ' active' : ''}`} onClick={() => setMode('login')}>Login</button>
        <button className={`tab-btn${mode === 'signup' ? ' active' : ''}`} onClick={() => setMode('signup')}>Sign Up</button>
      </div>

      <form onSubmit={handleSubmit}>
        <label className="field-label">Email</label>
        <input className="field-input" type="email" placeholder="you@example.com"
          value={email} onChange={e => setEmail(e.target.value)} autoComplete="email" required />

        <label className="field-label">Password</label>
        <input className="field-input" type="password" placeholder="••••••••"
          value={password} onChange={e => setPassword(e.target.value)}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />

        <button className="btn-primary" type="submit" disabled={loading} style={{ marginTop: 16 }}>
          {loading ? <span className="spinner" /> : (mode === 'login' ? 'Login' : 'Create Account')}
        </button>
      </form>
    </div>
  );
}

// ─── Dashboard ────────────────────────────────────────────────────────────────
function Dashboard({ userEmail, onLogout, pushToast }) {
  const [tab, setTab]                     = useState('pay');
  const [amount, setAmount]               = useState('500');
  const [orders, setOrders]               = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [payLoading, setPayLoading]       = useState(false);

  // Store pushToast in a ref so fetchOrders never needs it in its dep array.
  // Root cause of the "screen dances" bug: pushToast (plain function) was a
  // new reference every render → useCallback([pushToast]) saw a new dep →
  // fetchOrders recreated → useEffect([tab, fetchOrders]) fired again →
  // fetch → setState → re-render → repeat infinitely.
  const pushToastRef = useRef(pushToast);
  useEffect(() => { pushToastRef.current = pushToast; });

  const fetchOrders = useCallback(async () => {
    setLoadingOrders(true);
    try {
      const { data } = await API.get('/api/orders');
      setOrders(Array.isArray(data) ? data : []);
    } catch (err) {
      const status = err.response?.status;
      if (status === 401 || status === 403) {
        pushToastRef.current('Session expired — please log in again', 'error');
      } else {
        pushToastRef.current('Could not load orders', 'error');
      }
    } finally {
      setLoadingOrders(false);
    }
  }, []); // empty deps — fetchOrders is now stable forever

  useEffect(() => {
    if (tab === 'orders') fetchOrders();
  }, [tab, fetchOrders]);

  const handlePayment = async () => {
    const amt = parseFloat(amount);
    if (!amt || amt <= 0) { pushToast('Enter a valid amount', 'error'); return; }
    setPayLoading(true);
    try {
      const { data: order } = await API.post('/api/orders', { amount: amt });

      const options = {
        key: 'rzp_test_SjJbldo92o9fp3',
        amount: order.amount * 100,
        currency: 'INR',
        name: 'FlowPay',
        description: 'Secure Payment',
        order_id: order.razorpayOrderId,
        handler: async (response) => {
          try {
            await API.post('/api/orders/verify', {
              razorpayOrderId:   response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            pushToast('Payment verified! ✓', 'success');
            setTab('orders');
          } catch {
            pushToast('Payment verification failed', 'error');
          } finally {
            setPayLoading(false);
          }
        },
        modal: { ondismiss: () => setPayLoading(false) },
        prefill: { email: userEmail },
        theme: { color: '#6366f1' },
      };

      const razor = new window.Razorpay(options);
      razor.on('payment.failed', () => {
        pushToast('Payment failed', 'error');
        setPayLoading(false);
      });
      razor.open();
    } catch (err) {
      const raw = err.response?.data;
      const msg = (typeof raw === 'string' ? raw : raw?.message) || 'Failed to create order';
      pushToast(msg, 'error');
      setPayLoading(false);
    }
  };

  return (
    <div style={{ width: '100%', maxWidth: 580 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <div className="logo" style={{ fontSize: '1.6rem' }}>⚡ FlowPay</div>
          <div style={{ color: '#94a3b8', fontSize: 13, marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Icon d={icons.user} size={14} /> {userEmail}
          </div>
        </div>
        <button className="btn-ghost" onClick={onLogout} title="Logout">
          <Icon d={icons.logout} size={16} /> Logout
        </button>
      </div>

      {/* Tabs */}
      <div className="tab-row" style={{ marginBottom: 24 }}>
        <button className={`tab-btn${tab === 'pay' ? ' active' : ''}`} onClick={() => setTab('pay')}>
          <Icon d={icons.pay} size={15} /> Make Payment
        </button>
        <button className={`tab-btn${tab === 'orders' ? ' active' : ''}`} onClick={() => setTab('orders')}>
          <Icon d={icons.orders} size={15} /> My Orders
        </button>
      </div>

      {/* Pay Tab */}
      {tab === 'pay' && (
        <div className="card-glass">
          <h3 style={{ marginBottom: 20, fontWeight: 600, color: '#f1f5f9' }}>New Payment</h3>
          <label className="field-label">Amount (INR)</label>
          <div style={{ position: 'relative' }}>
            <span style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: '#94a3b8', fontWeight: 700 }}>₹</span>
            <input className="field-input" type="number" min="1" step="0.01"
              value={amount} onChange={e => setAmount(e.target.value)} style={{ paddingLeft: 32 }} />
          </div>
          <div style={{ color: '#94a3b8', fontSize: 13, marginBottom: 20, marginTop: 4 }}>
            You'll pay {amount ? formatCurrency(parseFloat(amount) || 0) : '—'}
          </div>
          <button className="btn-primary" onClick={handlePayment} disabled={payLoading}>
            {payLoading
              ? <><span className="spinner" /> Processing…</>
              : <><Icon d={icons.pay} size={16} /> Pay with Razorpay</>}
          </button>
          <p style={{ color: '#64748b', fontSize: 12, textAlign: 'center', marginTop: 12 }}>
            🔒 Secured by Razorpay · Test mode
          </p>
        </div>
      )}

      {/* Orders Tab */}
      {tab === 'orders' && (
        <div className="card-glass">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
            <h3 style={{ fontWeight: 600, color: '#f1f5f9', margin: 0 }}>Transaction History</h3>
            <button className="btn-ghost" onClick={fetchOrders} disabled={loadingOrders} style={{ fontSize: 13 }}>
              <Icon d={icons.refresh} size={14} /> Refresh
            </button>
          </div>

          {loadingOrders ? (
            <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748b' }}>
              <span className="spinner" style={{ width: 28, height: 28, borderWidth: 3 }} />
              <div style={{ marginTop: 12 }}>Loading orders…</div>
            </div>
          ) : orders.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748b' }}>
              <div style={{ fontSize: 40, marginBottom: 8 }}>🧾</div>
              <div>No transactions yet.</div>
              <div style={{ fontSize: 13, marginTop: 4 }}>Make a payment to get started.</div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {orders.map((o, i) => (
                <div key={o.id ?? i} className="order-row">
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, color: '#f1f5f9', fontSize: 15 }}>
                      {formatCurrency(o.amount)}
                    </div>
                    <div style={{ color: '#64748b', fontSize: 12, marginTop: 2 }}>
                      {formatDate(o.createdAt)} · <span style={{ fontFamily: 'monospace' }}>{o.razorpayOrderId ?? 'N/A'}</span>
                    </div>
                  </div>
                  <span style={{
                    background: `${statusColor(o.status)}22`,
                    color: statusColor(o.status),
                    border: `1px solid ${statusColor(o.status)}55`,
                    borderRadius: 8, padding: '3px 10px', fontSize: 12, fontWeight: 600,
                    whiteSpace: 'nowrap',
                  }}>
                    {o.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Root ─────────────────────────────────────────────────────────────────────
export default function App() {
  const [user, setUser]     = useState(() => {
    const token = localStorage.getItem('token');
    const email = localStorage.getItem('userEmail');
    return token && email ? email : null;
  });
  const [toasts, setToasts] = useState([]);

  const pushToast = useCallback((msg, type = 'success') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, msg, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000);
  }, []);

  const handleLogin = (email) => {
    localStorage.setItem('userEmail', email);
    setUser(email);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    setUser(null);
    pushToast('Logged out successfully', 'success');
  };

  return (
    <>
      <Toast toasts={toasts} />
      <div style={{
        minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '24px 16px',
      }}>
        {user
          ? <Dashboard userEmail={user} onLogout={handleLogout} pushToast={pushToast} />
          : <AuthPanel onLogin={handleLogin} pushToast={pushToast} />
        }
      </div>
    </>
  );
}