import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { orderApi } from "../api";
import { Empty, Loading, Notice, StatusPill } from "../components/ui";
import { useOrderSocket } from "../hooks/useOrderSocket";
import type { OrderDetail, OrderSummary } from "../types";
import { errorMessage, inr, when } from "../utils";

export function ConfirmationPage() {
  const { orderId } = useParams();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    orderApi.get(Number(orderId)).then(setOrder).catch((reason) => setError(errorMessage(reason, "The order could not be loaded.")));
  }, [orderId]);

  if (error) return <div className="mx-auto max-w-3xl px-4 py-10"><Notice>{error}</Notice></div>;
  if (!order) return <Loading label="Confirming your order" />;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <p className="text-sm text-sage">Order placed</p>
      <h1 className="font-display text-4xl">{order.orderNumber}</h1>
      <p className="mt-2 text-bark">{order.branchName}</p>
      <div className="mt-4 flex gap-2"><StatusPill status={order.status} /><StatusPill status={order.payment?.status ?? "PENDING"} /></div>
      <p className="mt-4 text-sm">Estimated preparation: {order.estimatedPrepMinutes} minutes. This is an estimate, not a promise.</p>
      <ul className="mt-6 space-y-2 text-sm">
        {order.items.map((item) => <li key={item.id}>{item.quantity} × {item.itemName}</li>)}
      </ul>
      <p className="mt-4 font-medium">{inr(order.totalAmount)} · {order.payment?.provider} payment {order.payment?.status.toLowerCase()}</p>
      <p className="mt-1 text-xs text-bark">Reference {order.payment?.providerReference}</p>
      <Link to={`/orders/${order.id}`} className="btn-primary mt-6">Track order</Link>
    </div>
  );
}

export function MyOrdersPage() {
  const [orders, setOrders] = useState<OrderSummary[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    orderApi.mine().then((page) => setOrders(page.content)).catch((reason) => setError(errorMessage(reason, "Orders could not be loaded.")));
  }, []);

  if (error) return <div className="mx-auto max-w-3xl px-4 py-10"><Notice>{error}</Notice></div>;
  if (!orders) return <Loading />;
  if (orders.length === 0) return <div className="mx-auto max-w-3xl px-4 py-10"><Empty title="No orders yet" body="When you place an order, it will appear here." /></div>;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="font-display text-4xl">My orders</h1>
      <ul className="mt-6 space-y-3">
        {orders.map((order) => (
          <li key={order.id}>
            <Link to={`/orders/${order.id}`} className="card flex items-center justify-between p-4">
              <div>
                <p className="font-medium">{order.orderNumber}</p>
                <p className="text-sm text-bark">{order.branchName} · {when(order.createdAt)}</p>
              </div>
              <div className="text-right">
                <StatusPill status={order.status} />
                <p className="mt-2 text-sm">{inr(order.totalAmount)}</p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function TrackingPage() {
  const { orderId } = useParams();
  const id = Number(orderId);
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    orderApi.tracking(id).then(setOrder).catch((reason) => setError(errorMessage(reason, "You are not authorized to access this order.")));
  }, [id]);
  const connected = useOrderSocket(order ? `/topic/orders/${order.id}` : null, load);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    if (connected) return;
    const timer = window.setInterval(load, 15000);
    return () => window.clearInterval(timer);
  }, [connected, load]);

  async function cancel() {
    try {
      setOrder(await orderApi.cancel(id));
    } catch (reason) {
      setError(errorMessage(reason, "This order could not be cancelled."));
    }
  }

  if (error && !order) return <div className="mx-auto max-w-3xl px-4 py-10"><Notice>{error}</Notice></div>;
  if (!order) return <Loading label="Loading tracking" />;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <p className="text-sm text-bark">{connected ? "Live updates connected" : "Kitchen connection lost. Reconnecting..."}</p>
      <h1 className="font-display text-4xl">{order.orderNumber}</h1>
      <p className="mt-2">{order.branchName} · {order.orderType.toLowerCase()}</p>
      <div className="mt-4"><StatusPill status={order.status} /></div>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      <ol className="mt-6 space-y-3">
        {order.history.map((entry, index) => (
          <li key={index} className="text-sm"><span className="font-medium">{entry.toStatus}</span> · {when(entry.createdAt)}{entry.note ? ` · ${entry.note}` : ""}</li>
        ))}
      </ol>
      <p className="mt-6 text-sm">Payment: {order.payment?.provider} {order.payment?.status} · {inr(order.totalAmount)}</p>
      {order.payment?.failureReason && <p className="mt-1 text-sm text-bark">{order.payment.failureReason}</p>}
      {order.status === "PLACED" && <button className="btn-secondary mt-6" onClick={cancel}>Cancel order</button>}
    </div>
  );
}
