import { Link } from "react-router-dom";
import { Empty, Notice } from "../components/ui";
import { useCart } from "../context/CartContext";
import { errorMessage, inr } from "../utils";
import { useState } from "react";

export function CartPage() {
  const { cart, updateItem, removeItem, clear } = useCart();
  const [error, setError] = useState("");

  async function change(id: number, quantity: number) {
    setError("");
    try {
      if (quantity < 1) await removeItem(id);
      else await updateItem(id, quantity);
    } catch (reason) {
      setError(errorMessage(reason, "The cart could not be updated."));
    }
  }

  if (!cart || cart.items.length === 0) {
    return <div className="mx-auto max-w-3xl px-4 py-10"><Empty title="Your cart is empty" body="Choose a Hearthline branch and add a plate." /><Link to="/restaurants" className="btn-primary mt-6">Browse restaurants</Link></div>;
  }

  return (
    <div className="mx-auto grid max-w-5xl gap-8 px-4 py-10 lg:grid-cols-[1.4fr_0.8fr]">
      <div>
        <h1 className="font-display text-4xl">Cart</h1>
        <p className="mt-2 text-sm text-bark">{cart.branch?.name}</p>
        {error && <div className="mt-4"><Notice>{error}</Notice></div>}
        <ul className="mt-6 space-y-4">
          {cart.items.map((item) => (
            <li key={item.id} className="card flex items-center justify-between gap-4 p-4">
              <div>
                <p className="font-medium">{item.name}</p>
                <p className="text-sm text-bark">{inr(item.unitPrice)} {item.available ? "" : "· unavailable"}</p>
              </div>
              <div className="flex items-center gap-3">
                <button className="btn-secondary px-3" onClick={() => change(item.id, item.quantity - 1)}>-</button>
                <span>{item.quantity}</span>
                <button className="btn-secondary px-3" onClick={() => change(item.id, item.quantity + 1)}>+</button>
                <button className="text-sm text-ember-dark" onClick={() => change(item.id, 0)}>Remove</button>
              </div>
            </li>
          ))}
        </ul>
        <button className="btn-ghost mt-4" onClick={() => clear().catch((reason) => setError(errorMessage(reason, "The cart could not be cleared.")))}>Clear cart</button>
      </div>
      <aside className="card h-fit p-5">
        <h2 className="font-display text-2xl">Summary</h2>
        <Row label="Subtotal" value={inr(cart.subtotal)} />
        <Row label="Tax" value={inr(cart.taxAmount)} />
        <Row label="Delivery if selected" value={inr(cart.deliveryFeeIfDelivery)} />
        <Row label="Pickup total" value={inr(cart.pickupTotal)} />
        <Row label="Delivery total" value={inr(cart.deliveryTotal)} />
        <Link to="/checkout" className="btn-primary mt-5 w-full">Checkout</Link>
      </aside>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return <p className="mt-3 flex justify-between text-sm"><span className="text-bark">{label}</span><span>{value}</span></p>;
}
