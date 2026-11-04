import { Link, NavLink } from "react-router-dom";
import { Menu, ShoppingBag, X } from "lucide-react";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

const links = [
  { to: "/", label: "Home" },
  { to: "/restaurants", label: "Restaurants" },
  { to: "/orders", label: "My Orders" }
];

export function SiteHeader() {
  const { user, logout } = useAuth();
  const { cart } = useCart();
  const [open, setOpen] = useState(false);
  const count = cart?.itemCount ?? 0;

  return (
    <header className="sticky top-0 z-20 border-b border-line/80 bg-cream/90 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <Link to="/" className="font-display text-2xl tracking-tight">Hearthline</Link>
        <nav className="hidden items-center gap-6 md:flex">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} className={({ isActive }) => `text-sm ${isActive ? "text-ember-dark" : "text-bark"}`}>
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          {user?.role === "CUSTOMER" && (
            <Link to="/cart" className="relative rounded-full p-2 hover:bg-ink/5" aria-label="Cart">
              <ShoppingBag size={20} />
              <span className="absolute -right-1 -top-1 grid h-5 min-w-5 place-items-center rounded-full bg-ember px-1 text-[11px] text-white">{count}</span>
            </Link>
          )}
          {user ? (
            <button className="btn-ghost hidden md:inline-flex" onClick={() => logout()}>Sign out</button>
          ) : (
            <Link to="/login" className="btn-primary hidden md:inline-flex">Login</Link>
          )}
          <button className="rounded-full p-2 md:hidden" onClick={() => setOpen((value) => !value)} aria-label="Menu">
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>
      {open && (
        <div className="space-y-2 border-t border-line px-4 py-3 md:hidden">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} onClick={() => setOpen(false)} className="block py-1 text-sm">{link.label}</NavLink>
          ))}
          {user ? <button className="btn-secondary w-full" onClick={() => logout()}>Sign out</button> : <Link to="/login" className="btn-primary w-full">Login</Link>}
        </div>
      )}
    </header>
  );
}

export function SiteFooter() {
  return (
    <footer className="mt-20 border-t border-line">
      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 md:grid-cols-3">
        <div>
          <p className="font-display text-2xl">Hearthline</p>
          <p className="mt-2 text-sm text-bark">Direct ordering for a small chain of fire-cooked kitchens.</p>
        </div>
        <div className="text-sm text-bark">
          <p className="font-medium text-ink">Visit</p>
          <Link className="mt-2 block" to="/restaurants">Locations</Link>
          <Link className="mt-1 block" to="/orders">My Orders</Link>
        </div>
        <div className="text-sm text-bark">
          <p className="font-medium text-ink">Kitchens</p>
          <p className="mt-2">Indiranagar, Bengaluru</p>
          <p>Bandra, Mumbai</p>
          <p>Connaught Place, New Delhi</p>
        </div>
      </div>
    </footer>
  );
}

export function Protected({ role, children }: { role?: UserRole; children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <p className="p-10 text-center text-bark">Checking your session...</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return children;
}

import { Navigate } from "react-router-dom";
import type { Role as UserRole } from "../types";
