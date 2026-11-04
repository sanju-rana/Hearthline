import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { cartApi } from "../api";
import type { Cart } from "../types";
import { useAuth } from "./AuthContext";

type CartValue = {
  cart: Cart | null;
  refresh: () => Promise<void>;
  setBranch: (branchId: number) => Promise<Cart>;
  addItem: (menuItemId: number, quantity: number) => Promise<Cart>;
  updateItem: (cartItemId: number, quantity: number) => Promise<Cart>;
  removeItem: (cartItemId: number) => Promise<Cart>;
  clear: () => Promise<Cart>;
};

const CartContext = createContext<CartValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [cart, setCart] = useState<Cart | null>(null);

  const refresh = useCallback(async () => {
    if (!user || user.role !== "CUSTOMER") {
      setCart(null);
      return;
    }
    setCart(await cartApi.get());
  }, [user]);

  useEffect(() => {
    refresh().catch(() => setCart(null));
  }, [refresh]);

  const value = useMemo<CartValue>(() => ({
    cart,
    refresh,
    setBranch: async (branchId) => {
      const next = await cartApi.setBranch(branchId);
      setCart(next);
      return next;
    },
    addItem: async (menuItemId, quantity) => {
      const next = await cartApi.add(menuItemId, quantity);
      setCart(next);
      return next;
    },
    updateItem: async (cartItemId, quantity) => {
      const next = await cartApi.update(cartItemId, quantity);
      setCart(next);
      return next;
    },
    removeItem: async (cartItemId) => {
      const next = await cartApi.remove(cartItemId);
      setCart(next);
      return next;
    },
    clear: async () => {
      const next = await cartApi.clear();
      setCart(next);
      return next;
    }
  }), [cart, refresh]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);
  if (!value) throw new Error("CartProvider is missing.");
  return value;
}
