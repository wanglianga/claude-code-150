import React, { useEffect, useMemo, useState } from 'react'
import { api, getUser } from '../api.js'
import { Card, Stats, Badge, fmt, useMeta } from '../ui.jsx'

export default function Consumption() {
  const { meta } = useMeta()
  const user = getUser()
  const [rows, setRows] = useState([])
  const [building, setBuilding] = useState('')
  const [process, setProcess] = useState('')
  const [crewId, setCrewId] = useState(user.role === 'ROLE_FOREMAN' ? String(user.crewId) : '')

  useEffect(() => {
    const q = new URLSearchParams()
    if (building) q.set('building', building)
    if (process) q.set('process', process)
    if (crewId) q.set('crewId', crewId)
    api.get('/api/analytics/consumption?' + q.toString()).then(setRows).catch(() => setRows([]))
  }, [building, process, crewId])

  const buildings = useMemo(() => [...new Set(meta.materials.length ? [] : [])], [meta]) // 由预算推导的选项在后端 meta
  const stats = useMemo(() => {
    const budget = rows.reduce((s, r) => s + r.budgetAmount, 0)
    const consumed = rows.reduce((s, r) => s + r.netConsumedAmount, 0)
    const waste = rows.reduce((s, r) => s + r.wasteAmount, 0)
    return { budget, consumed, waste, variance: consumed - budget }
  }, [rows])

  return (
    <div>
      <Stats items={[
        { label: '预算金额（筛选范围）', value: fmt(stats.budget), tone: 'blue' },
        { label: '实际净消耗（领用−退库）', value: fmt(stats.consumed), tone: 'brand' },
        { label: '材料偏差金额', value: fmt(stats.variance),
          sub: stats.variance > 0 ? '超耗（红）' : '节约（绿）',
          tone: stats.variance > 0 ? 'red' : 'green' },
        { label: '损耗金额', value: fmt(stats.waste), tone: stats.waste ? 'red' : 'green' }
      ]} />

      <Card title="按楼栋 / 工序 / 班组查看真实材料消耗与偏差"
            hint="项目经理口径：真实消耗=已发料−完好退库；偏差=实际净消耗−预算"
            actions={
              <div className="filters">
                <div className="f">
                  <label>楼栋</label>
                  <select value={building} onChange={(e) => setBuilding(e.target.value)}>
                    <option value="">全部楼栋</option>
                    {meta.buildings.map((b) => <option key={b}>{b}</option>)}
                  </select>
                </div>
                <div className="f">
                  <label>工序</label>
                  <select value={process} onChange={(e) => setProcess(e.target.value)}>
                    <option value="">全部工序</option>
                    {meta.processes.map((p) => <option key={p}>{p}</option>)}
                  </select>
                </div>
                {user.role !== 'ROLE_FOREMAN' && (
                  <div className="f">
                    <label>班组</label>
                    <select value={crewId} onChange={(e) => setCrewId(e.target.value)}>
                      <option value="">全部班组</option>
                      {meta.crews.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                )}
              </div>
            } flush>
        <div className="table-scroll">
          <table className="tbl">
            <thead><tr>
              <th>楼栋</th><th>工序</th><th>班组</th><th>材料</th>
              <th className="num">预算量</th><th className="num">领用量</th><th className="num">退库量</th>
              <th className="num">损耗量</th><th className="num">净消耗量</th>
              <th className="num">预算金额</th><th className="num">净消耗金额</th>
              <th className="num">偏差量</th><th className="num">偏差率</th><th>偏差判定</th>
            </tr></thead>
            <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td><b>{r.building}</b></td>
                <td>{r.process}</td>
                <td>{r.crewName || '—'}</td>
                <td>{r.materialName}<div className="muted" style={{ fontSize: 12 }}>{r.category}</div></td>
                <td className="num">{fmt(r.budgetQty)} {r.unit}</td>
                <td className="num">{fmt(r.issuedQty)}</td>
                <td className="num">{fmt(r.returnedQty)}</td>
                <td className="num">{r.wasteQty > 0 ? <span className="pos">{fmt(r.wasteQty)}</span> : 0}</td>
                <td className="num"><b>{fmt(r.netConsumedQty)}</b></td>
                <td className="num">{fmt(r.budgetAmount)}</td>
                <td className="num"><b>{fmt(r.netConsumedAmount)}</b></td>
                <td className={`num ${r.varianceQty > 0 ? 'pos' : 'neg'}`}>{fmt(r.varianceQty)}</td>
                <td className="num">{r.varianceRate === null ? '—' : `${r.varianceRate}%`}</td>
                <td>
                  {r.varianceAmount > 0
                    ? <Badge status="FAILED" label={`超耗 ${fmt(r.varianceAmount)}`} />
                    : <Badge status="PASSED" label={`节约 ${fmt(Math.abs(r.varianceAmount))}`} />}
                </td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={14}><div className="empty">当前筛选下暂无消耗数据</div></td></tr>}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
