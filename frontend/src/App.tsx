import { Navigate, Route, Routes } from "react-router-dom";
import { ClientLayout } from "./components/layout/ClientLayout";
import { ComingSoonPage } from "./pages/client/ComingSoonPage";
import { EditProfilePage } from "./pages/client/EditProfilePage";
import { HomePage } from "./pages/client/HomePage";
import { MenuPage } from "./pages/client/MenuPage";
import { MyOrdersPage } from "./pages/client/MyOrdersPage";
import { OrderStatusPage } from "./pages/client/OrderStatusPage";
import { ProfilePage } from "./pages/client/ProfilePage";
import { ReservePage } from "./pages/client/ReservePage";
import { SandboxPaymentPage } from "./pages/client/SandboxPaymentPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { StaffLayout, StaffHome, RequireSection } from "./pages/staff/StaffLayout";
import { StaffLoginPage } from "./pages/staff/StaffLoginPage";
import { StaffOrdersPage } from "./pages/staff/StaffOrdersPage";
import { KitchenPage } from "./pages/staff/KitchenPage";
import { CashierPage } from "./pages/staff/CashierPage";
import { DeliveryPage } from "./pages/staff/DeliveryPage";
import { DishesAdminPage } from "./pages/staff/DishesAdminPage";
import { MembersPage } from "./pages/staff/MembersPage";

export function App() {
  return (
    <Routes>
      <Route element={<ClientLayout />}>
        <Route index element={<HomePage />} />
        <Route path="menu" element={<MenuPage />} />
        <Route path="reservar" element={<ReservePage />} />
        <Route path="pago-simulado" element={<SandboxPaymentPage />} />
        <Route path="pedido/:id" element={<OrderStatusPage />} />
        <Route path="mis-reservas" element={<MyOrdersPage />} />
        <Route path="perfil" element={<ProfilePage />} />
        <Route path="perfil/editar" element={<EditProfilePage />} />
        <Route path="perfil/:opcion" element={<ComingSoonPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route path="staff/login" element={<StaffLoginPage />} />
      <Route path="staff" element={<StaffLayout />}>
        <Route index element={<StaffHome />} />
        <Route path="pedidos" element={<RequireSection section="pedidos"><StaffOrdersPage /></RequireSection>} />
        <Route path="cocina" element={<RequireSection section="cocina"><KitchenPage /></RequireSection>} />
        <Route path="caja" element={<RequireSection section="caja"><CashierPage /></RequireSection>} />
        <Route path="entrega" element={<RequireSection section="entrega"><DeliveryPage /></RequireSection>} />
        <Route path="menu" element={<RequireSection section="menu"><DishesAdminPage /></RequireSection>} />
        <Route path="personal" element={<RequireSection section="personal"><MembersPage /></RequireSection>} />
        <Route path="*" element={<Navigate to="/staff" replace />} />
      </Route>
    </Routes>
  );
}

