import { useEffect, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { adminApi, catalogApi } from "../api";
import { Empty, Loading, Notice, StatusPill } from "../components/ui";
import type { AdminMenu, Branch, Dashboard, OrderDetail, OrderSummary, Restaurant } from "../types";
import { errorMessage, inr, when } from "../utils";

export function AdminLayout() {
  const links = [
    ["/admin", "Overview"],
    ["/admin/branches", "Branches"],
    ["/admin/menu", "Menu"],
    ["/admin/orders", "Orders"]
  ];
  return (
    <div className="mx-auto grid max-w-6xl gap-6 px-4 py-8 md:grid-cols-[180px_1fr]">
      <aside className="space-y-2">
        <p className="font-display text-2xl">Admin</p>
        {links.map(([to, label]) => (
          <NavLink key={to} to={to} end={to === "/admin"} className={({ isActive }) => `block rounded-2xl px-3 py-2 text-sm ${isActive ? "bg-ink text-cream" : "text-bark"}`}>{label}</NavLink>
        ))}
      </aside>
      <div><Outlet /></div>
    </div>
  );
}

export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState("");
  useEffect(() => { adminApi.dashboard().then(setData).catch((reason) => setError(errorMessage(reason, "Dashboard could not be loaded."))); }, []);
  if (error) return <Notice>{error}</Notice>;
  if (!data) return <Loading />;
  const cards = [
    ["Total orders", data.totalOrders],
    ["Today", data.todaysOrders],
    ["Pending", data.pendingOrders],
    ["Completed", data.completedOrders],
    ["Revenue", inr(data.revenue)],
    ["Active branches", data.activeBranches],
    ["Available items", data.availableMenuItems]
  ];
  return (
    <div>
      <h1 className="font-display text-4xl">Overview</h1>
      <p className="mt-1 text-sm text-bark">Figures come from saved orders. Today uses Asia/Kolkata.</p>
      <div className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {cards.map(([label, value]) => (
          <div key={String(label)} className="card p-4"><p className="text-xs text-bark">{label}</p><p className="mt-1 text-2xl">{value}</p></div>
        ))}
      </div>
      <div className="card mt-6 h-72 p-4">
        <p className="mb-2 text-sm text-bark">Orders by status</p>
        <ResponsiveContainer width="100%" height="90%">
          <BarChart data={data.ordersByStatus}>
            <XAxis dataKey="status" tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="count" fill="#c2410c" radius={6} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export function BranchesPage() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    restaurantId: 0, name: "", addressLine: "", city: "", state: "", postalCode: "", phone: "", opensAt: "11:00", closesAt: "23:00", active: true, acceptingOrders: true
  });

  async function load() {
    const [nextBranches, nextRestaurants] = await Promise.all([adminApi.branches(), catalogApi.restaurants()]);
    setBranches(nextBranches);
    setRestaurants(nextRestaurants);
    setForm((current) => ({ ...current, restaurantId: current.restaurantId || nextRestaurants[0]?.id || 0 }));
  }

  useEffect(() => { load().catch((reason) => setError(errorMessage(reason, "Branches could not be loaded."))); }, []);

  return (
    <div>
      <h1 className="font-display text-4xl">Branches</h1>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      <form className="card mt-6 grid gap-3 p-4 md:grid-cols-2" onSubmit={async (event) => {
        event.preventDefault();
        try {
          await adminApi.createBranch({ ...form, opensAt: `${form.opensAt}:00`, closesAt: `${form.closesAt}:00` });
          await load();
        } catch (reason) {
          setError(errorMessage(reason, "The branch could not be saved."));
        }
      }}>
        <select className="field" value={form.restaurantId} onChange={(event) => setForm({ ...form, restaurantId: Number(event.target.value) })}>
          {restaurants.map((restaurant) => <option key={restaurant.id} value={restaurant.id}>{restaurant.name}</option>)}
        </select>
        <input className="field" placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
        <input className="field md:col-span-2" placeholder="Address" value={form.addressLine} onChange={(event) => setForm({ ...form, addressLine: event.target.value })} required />
        <input className="field" placeholder="City" value={form.city} onChange={(event) => setForm({ ...form, city: event.target.value })} required />
        <input className="field" placeholder="State" value={form.state} onChange={(event) => setForm({ ...form, state: event.target.value })} required />
        <input className="field" placeholder="Postal code" value={form.postalCode} onChange={(event) => setForm({ ...form, postalCode: event.target.value })} required />
        <input className="field" placeholder="Phone" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} required />
        <button className="btn-primary md:col-span-2" type="submit">Add branch</button>
      </form>
      <ul className="mt-6 space-y-3">
        {branches.map((branch) => (
          <li key={branch.id} className="card flex flex-wrap items-center justify-between gap-3 p-4">
            <div>
              <p className="font-medium">{branch.name}</p>
              <p className="text-sm text-bark">{branch.addressLine}, {branch.city}</p>
            </div>
            <div className="flex gap-2">
              <button className="btn-secondary" onClick={() => adminApi.operations(branch.id, !branch.active, branch.acceptingOrders).then(load)}>
                {branch.active ? "Deactivate" : "Activate"}
              </button>
              <button className="btn-secondary" onClick={() => adminApi.operations(branch.id, branch.active, !branch.acceptingOrders).then(load)}>
                {branch.acceptingOrders ? "Pause orders" : "Accept orders"}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function MenuAdminPage() {
  const [menu, setMenu] = useState<AdminMenu | null>(null);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ categoryId: 0, name: "", description: "", price: "199.00", vegetarian: true, popular: false, imageUrl: "", active: true });

  async function load() {
    const next = await adminApi.menu();
    setMenu(next);
    setForm((current) => ({ ...current, categoryId: current.categoryId || next.categories[0]?.id || 0 }));
  }
  useEffect(() => { load().catch((reason) => setError(errorMessage(reason, "Menu could not be loaded."))); }, []);
  if (!menu && !error) return <Loading />;

  return (
    <div>
      <h1 className="font-display text-4xl">Menu</h1>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      <form className="card mt-6 grid gap-3 p-4 md:grid-cols-2" onSubmit={async (event) => {
        event.preventDefault();
        try {
          await adminApi.createItem({ ...form, price: Number(form.price) });
          await load();
        } catch (reason) {
          setError(errorMessage(reason, "The dish could not be saved."));
        }
      }}>
        <select className="field" value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: Number(event.target.value) })}>
          {menu?.categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
        <input className="field" placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
        <input className="field md:col-span-2" placeholder="Description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} required />
        <input className="field" placeholder="Price" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} required />
        <input className="field" placeholder="Image URL" value={form.imageUrl} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} />
        <label className="text-sm"><input type="checkbox" checked={form.vegetarian} onChange={(event) => setForm({ ...form, vegetarian: event.target.checked })} /> Vegetarian</label>
        <label className="text-sm"><input type="checkbox" checked={form.popular} onChange={(event) => setForm({ ...form, popular: event.target.checked })} /> Popular</label>
        <button className="btn-primary md:col-span-2" type="submit">Add dish</button>
      </form>
      <ul className="mt-6 space-y-4">
        {menu?.items.map((item) => (
          <li key={item.id} className="card p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium">{item.name}</p>
                <p className="text-sm text-bark">{item.categoryName} · {inr(item.price)} · {item.active ? "Active" : "Inactive"}</p>
              </div>
              {item.active && <button className="btn-secondary" onClick={() => adminApi.deactivateItem(item.id).then(load)}>Deactivate</button>}
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              {item.availability.map((row) => (
                <button key={row.branchId} className="btn-secondary" onClick={() => adminApi.availability(row.branchId, item.id, !row.available).then(load)}>
                  {row.branchName.replace("Hearthline ", "")}: {row.available ? "on" : "off"}
                </button>
              ))}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function OrdersAdminPage() {
  const [status, setStatus] = useState("");
  const [orderNumber, setOrderNumber] = useState("");
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [selected, setSelected] = useState<OrderDetail | null>(null);
  const [error, setError] = useState("");

  async function load(nextStatus = status, nextNumber = orderNumber) {
    const page = await adminApi.orders({ status: nextStatus || undefined, orderNumber: nextNumber || undefined, size: 20 });
    setOrders(page.content);
  }
  useEffect(() => { load().catch((reason) => setError(errorMessage(reason, "Orders could not be loaded."))); }, []);

  return (
    <div>
      <h1 className="font-display text-4xl">Orders</h1>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      <div className="mt-4 flex gap-2">
        <input className="field" placeholder="Order number" value={orderNumber} onChange={(event) => setOrderNumber(event.target.value)} />
        <select className="field" value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">All statuses</option>
          {["PLACED", "ACCEPTED", "PREPARING", "READY", "COMPLETED", "REJECTED", "CANCELLED"].map((value) => <option key={value}>{value}</option>)}
        </select>
        <button className="btn-primary" onClick={() => load().catch((reason) => setError(errorMessage(reason, "Orders could not be loaded.")))}>Search</button>
      </div>
      {orders.length === 0 ? <div className="mt-6"><Empty title="No orders" body="Orders appear here after a customer checks out." /></div> : (
        <ul className="mt-6 space-y-3">
          {orders.map((order) => (
            <li key={order.id}>
              <button className="card flex w-full items-center justify-between p-4 text-left" onClick={() => adminApi.order(order.id).then(setSelected)}>
                <span>{order.orderNumber}<span className="mt-1 block text-sm text-bark">{order.branchName} · {when(order.createdAt)}</span></span>
                <StatusPill status={order.status} />
              </button>
            </li>
          ))}
        </ul>
      )}
      {selected && (
        <div className="card mt-6 p-4 text-sm">
          <p className="font-medium">{selected.orderNumber}</p>
          <p className="text-bark">{selected.customerName} · {selected.customerPhone}</p>
          <ul className="mt-3 space-y-1">{selected.items.map((item) => <li key={item.id}>{item.quantity} × {item.itemName}</li>)}</ul>
          <p className="mt-3">{inr(selected.totalAmount)} · {selected.payment?.provider} {selected.payment?.status}</p>
        </div>
      )}
    </div>
  );
}
