import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { authApi } from "../api";
import type { User } from "../types";

type AuthValue = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (body: { fullName: string; email: string; password: string; phone: string }) => Promise<User>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    authApi.me().then(setUser).catch(() => setUser(null)).finally(() => setLoading(false));
  }, []);

  const value = useMemo<AuthValue>(() => ({
    user,
    loading,
    login: async (email, password) => {
      const next = await authApi.login(email, password);
      setUser(next);
      return next;
    },
    register: async (body) => {
      const next = await authApi.register(body);
      setUser(next);
      return next;
    },
    logout: async () => {
      await authApi.logout();
      setUser(null);
    }
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthProvider is missing.");
  return value;
}
