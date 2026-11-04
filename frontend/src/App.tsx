import { Navigate, Route, Routes } from "react-router-dom";
import { Protected, SiteFooter, SiteHeader } from "./components/SiteChrome";
import { AdminLayout, BranchesPage, DashboardPage, MenuAdminPage, OrdersAdminPage } from "./admin/AdminPages";
import { KitchenPage } from "./kitchen/KitchenPage";
import { LoginPage, RegisterPage } from "./pages/AuthPages";
import { CartPage } from "./pages/CartPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { HomePage } from "./pages/HomePage";
import { ItemPage } from "./pages/ItemPage";
import { MenuPage } from "./pages/MenuPage";
import { ConfirmationPage, MyOrdersPage, TrackingPage } from "./pages/OrderPages";
import { RestaurantsPage } from "./pages/RestaurantsPage";

function CustomerShell({ children }: { children: React.ReactNode }) {
  return (
    <>
      <SiteHeader />
      <main>{children}</main>
      <SiteFooter />
    </>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<CustomerShell><HomePage /></CustomerShell>} />
      <Route path="/restaurants" element={<CustomerShell><RestaurantsPage /></CustomerShell>} />
      <Route path="/branches/:branchId" element={<CustomerShell><MenuPage /></CustomerShell>} />
      <Route path="/branches/:branchId/items/:itemId" element={<CustomerShell><ItemPage /></CustomerShell>} />
      <Route path="/login" element={<CustomerShell><LoginPage /></CustomerShell>} />
      <Route path="/register" element={<CustomerShell><RegisterPage /></CustomerShell>} />
      <Route path="/cart" element={<CustomerShell><Protected role="CUSTOMER"><CartPage /></Protected></CustomerShell>} />
      <Route path="/checkout" element={<CustomerShell><Protected role="CUSTOMER"><CheckoutPage /></Protected></CustomerShell>} />
      <Route path="/orders" element={<CustomerShell><Protected role="CUSTOMER"><MyOrdersPage /></Protected></CustomerShell>} />
      <Route path="/orders/:orderId" element={<CustomerShell><Protected role="CUSTOMER"><TrackingPage /></Protected></CustomerShell>} />
      <Route path="/orders/:orderId/confirmation" element={<CustomerShell><Protected role="CUSTOMER"><ConfirmationPage /></Protected></CustomerShell>} />
      <Route path="/kitchen" element={<Protected role="KITCHEN_STAFF"><KitchenPage /></Protected>} />
      <Route path="/admin" element={<Protected role="RESTAURANT_ADMIN"><CustomerShell><AdminLayout /></CustomerShell></Protected>}>
        <Route index element={<DashboardPage />} />
        <Route path="branches" element={<BranchesPage />} />
        <Route path="menu" element={<MenuAdminPage />} />
        <Route path="orders" element={<OrdersAdminPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
