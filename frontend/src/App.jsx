import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { MENUS, MENU_META } from './menus.js'
import Login from './pages/Login.jsx'
import MainLayout from './pages/MainLayout.jsx'
import InventoryPage from './pages/InventoryPage.jsx'
import StockCheckPage from './pages/StockCheckPage.jsx'
import RequisitionPage from './pages/RequisitionPage.jsx'
import ReturnPage from './pages/ReturnPage.jsx'
import LossPage from './pages/LossPage.jsx'
import ChangePage from './pages/ChangePage.jsx'
import BudgetPage from './pages/BudgetPage.jsx'
import SettlementPage from './pages/SettlementPage.jsx'
import DeviationPage from './pages/DeviationPage.jsx'
import FinancePage from './pages/FinancePage.jsx'
import ProcurementPage from './pages/ProcurementPage.jsx'
import LossReviewPage from './pages/LossReviewPage.jsx'
import MasterPage from './pages/MasterPage.jsx'

export default function App() {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null')
    } catch {
      return null
    }
  })

  const allowed = useMemo(() => (user ? MENUS[user.role] || [] : []), [user])

  return (
    <HashRouter>
      <Routes>
        <Route path="/login" element={<Login onLogin={setUser} />} />
        <Route path="/" element={user ? <MainLayout user={user} menus={allowed} /> : <Navigate to="/login" />}>
          <Route index element={<Navigate to={MENU_META.find((m) => allowed.includes(m.key))?.path || '/login'} replace />} />
          {allowed.includes('inventory') && <Route path="inventory" element={<InventoryPage />} />}
          {allowed.includes('stockChecks') && <Route path="stock-checks" element={<StockCheckPage />} />}
          {allowed.includes('requisitions') && <Route path="requisitions" element={<RequisitionPage user={user} />} />}
          {allowed.includes('returns') && <Route path="returns" element={<ReturnPage user={user} />} />}
          {allowed.includes('losses') && <Route path="losses" element={<LossPage user={user} />} />}
          {allowed.includes('changes') && <Route path="changes" element={<ChangePage />} />}
          {allowed.includes('budgets') && <Route path="budgets" element={<BudgetPage />} />}
          {allowed.includes('settlement') && <Route path="settlement" element={<SettlementPage />} />}
          {allowed.includes('deviation') && <Route path="deviation" element={<DeviationPage />} />}
          {allowed.includes('finance') && <Route path="finance" element={<FinancePage />} />}
          {allowed.includes('procurement') && <Route path="procurement" element={<ProcurementPage />} />}
          {allowed.includes('lossReview') && <Route path="loss-review" element={<LossReviewPage />} />}
          {allowed.includes('masters') && <Route path="masters" element={<MasterPage />} />}
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </HashRouter>
  )
}
