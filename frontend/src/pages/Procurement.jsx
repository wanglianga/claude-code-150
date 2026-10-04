import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Card, Stats, Badge, fmt, useMeta } from '../ui.jsx'

export default function Procurement() {
  const { meta } = useMeta()
  const [building, setBuilding] = useState('')
  const [data, setData] = useState(null)

  useEffect(() => {
    api.get('/api/analytics/procurement-plan' + (building ? `?building=${encodeURIComponent(building)}` : ''))
      .then(setData).catch(() => {})
  }, [building])

  const rows = data?.rows || []
  const needBuy = rows.filter((r) => r.suggestPurchaseQty > 0)
  const risk = rows.filter((r) => r.supplierDelayRisk)

  return (
    <div>
      <Stats items={[
        { label: '建议采购总额（元）', value: fmt(data?.totalSuggestAmount), tone: 'brand' },
        { label: '需采购材料种类', value: needBuy.length, tone: 'blue' },
        { label: '补货延迟/质量风险', value: risk.length, tone: risk.length ? 'red' : 'green' },
        { label: '覆盖楼栋', value: building || '全部楼栋', tone: '' }
      ]} />

      <Card title="以实际消耗反推后续采购计划"
            hint="建议采购量 = max(0, 预算总量 − 实际净消耗 − 可用库存)；单价取历史批次均价，可用于分包结算量复核"
            actions={
              <div className="filters">
                <div className="f">
                  <label>楼栋</label>
                  <select value={building} onChange={(e) => setBuilding(e.target.value)}>
                    <option value="">全部楼栋</option>
                    {meta.buildings.map((b) => <option key={b}>{b}</option>)}
                  </select>
                </div>
              </div>
            } flush>
        <div className="table-scroll">
          <table className="tbl">
            <thead><tr>
              <th>材料</th><th>类别</th><th>规格</th>
              <th className="num">预算总量</th><th className="num">已净消耗</th>
              <th className="num">剩余需求</th><th className="num">可用库存</th>
              <th className="num">建议采购量</th><th className="num">历史均价</th>
              <th className="num">建议采购金额</th><th>建议供应商</th><th>供应风险</th>
            </tr></thead>
            <tbody>
            {rows.map((r) => (
              <tr key={r.materialId} style={r.suggestPurchaseQty > 0 ? { background: '#fffbeb' } : undefined}>
                <td><b>{r.materialName}</b></td>
                <td>{r.category}</td>
                <td>{r.spec}</td>
                <td className="num">{fmt(r.budgetQty)} {r.unit}</td>
                <td className="num">{fmt(r.consumedQty)}</td>
                <td className="num">{fmt(r.remainingDemand)}</td>
                <td className="num">{fmt(r.usableStock)}</td>
                <td className="num"><b style={{ color: r.suggestPurchaseQty > 0 ? 'var(--brand-dark)' : undefined }}>
                  {fmt(r.suggestPurchaseQty)}
                </b></td>
                <td className="num">{fmt(r.avgPrice)}</td>
                <td className="num">{fmt(r.suggestAmount)}</td>
                <td>{r.suggestSupplier || '—'}</td>
                <td>{r.supplierDelayRisk
                  ? <Badge status="FAILED" label="补货延迟/质量风险" />
                  : <Badge status="PASSED" label="正常" />}</td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      </Card>
      <Card title="分包结算复核口径">
        <div className="kpi-note">
          1) 以"实际净消耗量"对照图纸预算量，偏差为正且无签证支撑的部分，在分包结算中扣回；<br />
          2) 有<b>变更签证</b>支撑的增量（如地下室卷材 +200㎡）按签证单价进入结算与索赔；<br />
          3) 建议采购量用于材料员编制后续采购批次，避免砂浆等时效性材料一次进场过多造成过期损耗。
        </div>
      </Card>
    </div>
  )
}
