import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { catalogApi } from "../api";
import { DietMark, FoodImage, Loading, Notice } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { Branch, MenuItem } from "../types";
import { errorMessage, inr } from "../utils";

export function ItemPage() {
  const { branchId, itemId } = useParams();
  const id = Number(branchId);
  const navigate = useNavigate();
  const { user } = useAuth();
  const { cart, setBranch, addItem } = useCart();
  const [branch, setBranchState] = useState<Branch | null>(null);
  const [item, setItem] = useState<MenuItem | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([catalogApi.branch(id), catalogApi.item(Number(itemId), id)])
      .then(([nextBranch, nextItem]) => {
        setBranchState(nextBranch);
        setItem(nextItem);
      })
      .catch((reason) => setError(errorMessage(reason, "This dish could not be loaded.")));
  }, [id, itemId]);

  async function add() {
    if (!item) return;
    if (!user || user.role !== "CUSTOMER") {
      navigate("/login");
      return;
    }
    try {
      if (cart?.branch && cart.branch.id !== id && cart.items.length > 0) {
        const confirmed = window.confirm("Your cart belongs to another branch. Switching will clear it.");
        if (!confirmed) return;
      }
      if (!cart?.branch || cart.branch.id !== id) await setBranch(id);
      await addItem(item.id, quantity);
      navigate("/cart");
    } catch (reason) {
      setError(errorMessage(reason, "This menu item is currently unavailable."));
    }
  }

  if (error && !item) return <div className="mx-auto max-w-3xl px-4 py-10"><Notice>{error}</Notice></div>;
  if (!item || !branch) return <Loading />;

  return (
    <div className="mx-auto grid max-w-5xl gap-8 px-4 py-10 md:grid-cols-2">
      <div className="card h-80 overflow-hidden"><FoodImage src={item.imageUrl} alt={item.name} /></div>
      <div>
        <Link to={`/branches/${id}`} className="text-sm text-ember-dark">{branch.name}</Link>
        <h1 className="mt-2 font-display text-4xl">{item.name}</h1>
        <p className="mt-3 text-bark">{item.description}</p>
        <div className="mt-4"><DietMark vegetarian={item.vegetarian} /></div>
        <p className="mt-4 text-2xl">{inr(item.price)}</p>
        <p className="mt-2 text-sm">{item.available && branch.acceptingOrders ? "Available" : "Unavailable"}</p>
        <div className="mt-5 flex items-center gap-3">
          <button className="btn-secondary" onClick={() => setQuantity((value) => Math.max(1, value - 1))}>-</button>
          <span>{quantity}</span>
          <button className="btn-secondary" onClick={() => setQuantity((value) => Math.min(20, value + 1))}>+</button>
        </div>
        {error && <div className="mt-4"><Notice>{error}</Notice></div>}
        <button className="btn-primary mt-6" disabled={!item.available || !branch.acceptingOrders} onClick={add}>Add to cart</button>
      </div>
    </div>
  );
}
