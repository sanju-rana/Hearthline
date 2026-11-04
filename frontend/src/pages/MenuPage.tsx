import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { catalogApi } from "../api";
import { DietMark, Empty, FoodImage, Loading, Notice } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { Branch, Category, MenuItem } from "../types";
import { errorMessage, inr } from "../utils";

export function MenuPage() {
  const { branchId } = useParams();
  const id = Number(branchId);
  const navigate = useNavigate();
  const { user } = useAuth();
  const { cart, setBranch, addItem } = useCart();
  const [branch, setBranchState] = useState<Branch | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [items, setItems] = useState<MenuItem[] | null>(null);
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [sort, setSort] = useState("");
  const [availableOnly, setAvailableOnly] = useState(false);
  const [popularOnly, setPopularOnly] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    catalogApi.branch(id).then(setBranchState).catch((reason) => setError(errorMessage(reason, "Branch not found.")));
    catalogApi.categories().then(setCategories).catch(() => setCategories([]));
  }, [id]);

  useEffect(() => {
    catalogApi.menu(id, {
      search: search || undefined,
      categoryId: categoryId || undefined,
      sort: sort || undefined,
      availableOnly,
      popularOnly
    }).then(setItems).catch((reason) => setError(errorMessage(reason, "Menu could not be loaded.")));
  }, [id, search, categoryId, sort, availableOnly, popularOnly]);

  async function add(item: MenuItem) {
    setNotice("");
    setError("");
    if (!user) {
      navigate("/login");
      return;
    }
    if (user.role !== "CUSTOMER") {
      setError("Sign in with a customer account to order.");
      return;
    }
    try {
      if (cart?.branch && cart.branch.id !== id && cart.items.length > 0) {
        const confirmed = window.confirm("Your cart belongs to another branch. Switching will clear it.");
        if (!confirmed) return;
      }
      if (!cart?.branch || cart.branch.id !== id) await setBranch(id);
      await addItem(item.id, 1);
      setNotice(`${item.name} added to your cart.`);
    } catch (reason) {
      setError(errorMessage(reason, "This item could not be added."));
    }
  }

  if (error && !branch) return <div className="mx-auto max-w-6xl px-4 py-10"><Notice>{error}</Notice></div>;
  if (!branch || !items) return <Loading label="Loading the menu" />;

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <p className="text-sm text-bark">{branch.city}</p>
      <h1 className="font-display text-4xl">{branch.name}</h1>
      <p className="mt-2 text-sm text-bark">{branch.acceptingOrders ? "Accepting orders" : "This kitchen is not accepting orders right now."}</p>
      <div className="mt-6 grid gap-3 md:grid-cols-4">
        <input className="field md:col-span-2" placeholder="Search the menu" value={search} onChange={(event) => setSearch(event.target.value)} />
        <select className="field" value={categoryId} onChange={(event) => setCategoryId(event.target.value ? Number(event.target.value) : "")}>
          <option value="">All categories</option>
          {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
        <select className="field" value={sort} onChange={(event) => setSort(event.target.value)}>
          <option value="">Sort by name</option>
          <option value="priceAsc">Price: low to high</option>
          <option value="priceDesc">Price: high to low</option>
        </select>
      </div>
      <div className="mt-3 flex gap-4 text-sm">
        <label className="flex items-center gap-2"><input type="checkbox" checked={availableOnly} onChange={(event) => setAvailableOnly(event.target.checked)} />Available only</label>
        <label className="flex items-center gap-2"><input type="checkbox" checked={popularOnly} onChange={(event) => setPopularOnly(event.target.checked)} />Popular</label>
      </div>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      {notice && <p className="mt-4 text-sm text-sage">{notice}</p>}
      {items.length === 0 ? <div className="mt-8"><Empty title="Nothing matches" body="Try another search or category." /></div> : (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item) => (
            <article key={item.id} className="card overflow-hidden">
              <Link to={`/branches/${id}/items/${item.id}`} className="block h-44"><FoodImage src={item.imageUrl} alt={item.name} /></Link>
              <div className="p-4">
                <p className="text-xs text-bark">{item.categoryName}</p>
                <h2 className="font-display text-2xl"><Link to={`/branches/${id}/items/${item.id}`}>{item.name}</Link></h2>
                <p className="mt-1 line-clamp-2 text-sm text-bark">{item.description}</p>
                <div className="mt-3 flex items-center justify-between">
                  <DietMark vegetarian={item.vegetarian} />
                  <span className="font-medium">{inr(item.price)}</span>
                </div>
                <button className="btn-primary mt-4 w-full" disabled={!item.available || !branch.acceptingOrders} onClick={() => add(item)}>
                  {item.available ? "Add to cart" : "Unavailable"}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
