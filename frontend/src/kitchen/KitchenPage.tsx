import { useCallback, useEffect, useRef, useState } from "react";
import { kitchenApi } from "../api";
import { useOrderSocket } from "../hooks/useOrderSocket";
import { useAuth } from "../context/AuthContext";
import type { KitchenOrder, OrderStatus } from "../types";
import { elapsed, errorMessage, inr } from "../utils";

const lanes: { label: string; status?: OrderStatus; next?: OrderStatus; action?: string }[] = [
  { label: "New", status: "PLACED", next: "ACCEPTED", action: "Accept" },
  { label: "Accepted", status: "ACCEPTED", next: "PREPARING", action: "Start preparing" },
  { label: "Preparing", status: "PREPARING", next: "READY", action: "Mark ready" },
  { label: "Ready", status: "READY", next: "COMPLETED", action: "Complete" },
  { label: "Completed", status: "COMPLETED" }
];

export function KitchenPage() {
  const { user } = useAuth();
  const [lane, setLane] = useState<OrderStatus>("PLACED");
  const [orders, setOrders] = useState<KitchenOrder[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [now, setNow] = useState(Date.now());
  const [soundOn, setSoundOn] = useState(false);
  const seen = useRef<number[]>([]);
  const sound = useRef(soundOn);
  sound.current = soundOn;

  const load = useCallback(() => {
    kitchenApi.list(lane)
      .then((next) => {
        setOrders(next);
        setError("");
        if (lane === "PLACED" && sound.current) {
          const fresh = next.some((order) => !seen.current.includes(order.id));
          if (fresh && seen.current.length > 0) beep();
        }
        seen.current = next.map((order) => order.id);
      })
      .catch((reason) => setError(errorMessage(reason, "Orders could not be loaded.")))
      .finally(() => setLoading(false));
  }, [lane]);

  const connected = useOrderSocket(user?.branchId ? `/topic/kitchen/${user.branchId}` : null, load);

  useEffect(() => {
    seen.current = [];
    setLoading(true);
    load();
  }, [load]);
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  useEffect(() => {
    if (connected) return;
    const timer = window.setInterval(load, 15000);
    return () => window.clearInterval(timer);
  }, [connected, load]);

  async function move(order: KitchenOrder, status: OrderStatus) {
    setError("");
    try {
      await kitchenApi.update(order.id, status);
      await load();
    } catch (reason) {
      setError(errorMessage(reason, "This order cannot move to that status."));
    }
  }

  const current = lanes.find((item) => item.status === lane);

  return (
    <div className="min-h-screen bg-[#1a1411] text-[#f6f0e8]">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 px-4 py-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-white/50">Kitchen</p>
          <h1 className="font-display text-3xl">{user?.branchName}</h1>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <span>{connected ? "Connected" : "Kitchen connection lost. Reconnecting..."}</span>
          <button className="rounded-full border border-white/20 px-3 py-1" onClick={() => setSoundOn((value) => !value)}>
            {soundOn ? "Sound on" : "Enable sound"}
          </button>
        </div>
      </header>
      <div className="flex gap-2 overflow-x-auto px-4 py-4">
        {lanes.map((item) => (
          <button key={item.label} className={`rounded-full px-4 py-2 text-sm ${lane === item.status ? "bg-[#f6f0e8] text-[#1a1411]" : "bg-white/10"}`} onClick={() => { setLoading(true); setLane(item.status!); }}>
            {item.label}
          </button>
        ))}
      </div>
      {error && <p className="px-4 text-sm text-red-300">{error}</p>}
      {loading ? <p className="px-4 py-10 text-white/70">Loading tickets...</p> : orders.length === 0 ? (
        <p className="px-4 py-16 text-center text-lg text-white/70">No tickets in this lane.</p>
      ) : (
        <div className="grid gap-4 px-4 pb-10 md:grid-cols-2 xl:grid-cols-3">
          {orders.map((order) => (
            <article key={order.id} className="rounded-3xl bg-[#2a211c] p-5">
              <div className="flex items-start justify-between">
                <h2 className="font-display text-3xl">{order.orderNumber}</h2>
                <span className="text-sm text-white/70">{elapsed(order.createdAt, now)}</span>
              </div>
              <p className="mt-1 text-sm uppercase tracking-wide text-amber-200">{order.orderType}</p>
              <ul className="mt-4 space-y-2 text-lg">
                {order.items.map((item) => <li key={item.id}>{item.quantity} × {item.itemName}</li>)}
              </ul>
              {order.specialInstructions && <p className="mt-4 rounded-2xl bg-black/20 p-3 text-sm">Note: {order.specialInstructions}</p>}
              <p className="mt-4 text-sm text-white/60">{inr(order.totalAmount)} · about {order.estimatedPrepMinutes} min</p>
              <div className="mt-4 flex gap-2">
                {current?.next && current.action && (
                  <button className="rounded-full bg-[#f6f0e8] px-4 py-2 text-sm font-medium text-[#1a1411]" onClick={() => move(order, current.next!)}>{current.action}</button>
                )}
                {order.status === "PLACED" && (
                  <button className="rounded-full border border-white/20 px-4 py-2 text-sm" onClick={() => move(order, "REJECTED")}>Reject</button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

function beep() {
  const context = new AudioContext();
  const oscillator = context.createOscillator();
  const gain = context.createGain();
  oscillator.frequency.value = 880;
  gain.gain.value = 0.05;
  oscillator.connect(gain);
  gain.connect(context.destination);
  oscillator.start();
  oscillator.stop(context.currentTime + 0.18);
}
