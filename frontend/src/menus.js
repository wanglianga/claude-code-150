// 各角色可见菜单（key 与路由对应）
export const MENUS = {
  ADMIN: ['requisitions', 'returns', 'losses', 'changes', 'inventory', 'stockChecks', 'budgets',
    'settlement', 'deviation', 'finance', 'procurement', 'lossReview', 'masters'],
  WAREHOUSE: ['inventory', 'stockChecks', 'requisitions', 'returns', 'losses', 'masters'],
  CREW_LEADER: ['requisitions', 'returns', 'losses'],
  PROJECT_MANAGER: ['deviation', 'requisitions', 'settlement', 'changes', 'lossReview'],
  FINANCE: ['finance', 'settlement', 'losses', 'changes'],
  BUDGET: ['budgets', 'procurement', 'settlement', 'deviation']
}

export const MENU_META = [
  { key: 'requisitions', path: '/requisitions', label: '领料单', icon: '📋' },
  { key: 'returns', path: '/returns', label: '退库单', icon: '↩️' },
  { key: 'losses', path: '/losses', label: '损耗登记', icon: '⚠️' },
  { key: 'changes', path: '/changes', label: '变更签证', icon: '📝' },
  { key: 'inventory', path: '/inventory', label: '入库与库存', icon: '🏗️' },
  { key: 'stockChecks', path: '/stock-checks', label: '库存盘点', icon: '🧮' },
  { key: 'budgets', path: '/budgets', label: '预算与进度', icon: '🎯' },
  { key: 'settlement', path: '/settlement', label: '月度班组核算', icon: '💰' },
  { key: 'deviation', path: '/deviation', label: '材料偏差分析', icon: '📊' },
  { key: 'finance', path: '/finance', label: '财务结算', icon: '🏦' },
  { key: 'procurement', path: '/procurement', label: '采购反推/分包', icon: '🛒' },
  { key: 'lossReview', path: '/loss-review', label: '损耗复盘', icon: '🔎' },
  { key: 'masters', path: '/masters', label: '基础数据', icon: '🗂️' }
]
