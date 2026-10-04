import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Card, Modal, Field, fmt, useToast, useMeta } from '../ui.jsx'

export default function Budgets() {
  const toast = useToast()
  const { meta } = useMeta()
  const [rows, setRows] = useState([])
  const [show, setShow] = useState(false)
  const [f, setF] = useState({
    building: '1#楼', area: '', process: '', drawingNo: '', materialId: '',
    budgetQty: '', budgetPrice: '', crewId: ''
  })

  const load = () => api.get('/api/budgets').then(setRows)
  useEffect(() => { load() }, [])

  const submit = async () => {
    if (!f.materialId || !f.process || !f.budgetQty) return toast('请填写楼栋、工序、材料与预算量', 'err')
    try {
      await api.post('/api/budgets', {
        ...f,
        materialId: Number(f.materialId),
        crewId: f.crewId ? Number(f.crewId) : null,
        budgetQty: Number(f.budgetQty),
        budgetPrice: f.budgetPrice === '' ? 0 : Number(f.budgetPrice)
      })
      toast('预算已录入'); setShow(false); load()
    } catch (e) { toast(e.message, 'err') }
  }

  const nameOf = (id) => meta.materials.find((m) => m.id === id)
  const total = rows.reduce((s, b) => s + b.budgetQty * (b.budgetPrice || 0), 0)

  return (
    <Card title={`工程材料预算（${rows.length} 行）`}
          hint="按 楼栋/施工区域/工序/图纸 + 材料 编制，作为发料核对与班组核算基准"
          actions={<button className="btn primary" onClick={() => setShow(true)}>＋ 编制预算</button>}
          flush>
      <div className="card-bd kpi-note">预算总金额（预算口径单价）：<b style={{ fontSize: 15 }}>{fmt(total)} 元</b></div>
      <div className="table-scroll">
        <table className="tbl">
          <thead><tr>
            <th>楼栋</th><th>施工区域</th><th>工序</th><th>图纸</th><th>材料</th>
            <th>归口班组</th><th className="num">预算量</th><th className="num">预算单价</th>
            <th className="num">预算金额</th>
          </tr></thead>
          <tbody>
          {rows.map((b) => {
            const m = nameOf(b.materialId)
            return (
              <tr key={b.id}>
                <td><b>{b.building}</b></td>
                <td>{b.area}</td><td>{b.process}</td><td>{b.drawingNo}</td>
                <td>{m?.name}<div className="muted" style={{ fontSize: 12 }}>{m?.spec}</div></td>
                <td>{meta.crews.find((c) => c.id === b.crewId)?.name || '共用'}</td>
                <td className="num">{fmt(b.budgetQty)} {m?.unit}</td>
                <td className="num">{fmt(b.budgetPrice)}</td>
                <td className="num"><b>{fmt(b.budgetQty * (b.budgetPrice || 0))}</b></td>
              </tr>
            )
          })}
          </tbody>
        </table>
      </div>

      {show && (
        <Modal title="编制材料预算" onClose={() => setShow(false)}
               footer={<><button className="btn" onClick={() => setShow(false)}>取消</button>
                 <button className="btn primary" onClick={submit}>保存预算</button></>}>
          <div className="grid2">
            <Field label="楼栋 *"><input value={f.building} onChange={(e) => setF({ ...f, building: e.target.value })} /></Field>
            <Field label="施工区域"><input value={f.area} onChange={(e) => setF({ ...f, area: e.target.value })} placeholder="A区/地下室/标准层" /></Field>
            <Field label="工序 *"><input value={f.process} onChange={(e) => setF({ ...f, process: e.target.value })} placeholder="主体结构/防水工程…" /></Field>
            <Field label="图纸编号"><input value={f.drawingNo} onChange={(e) => setF({ ...f, drawingNo: e.target.value })} placeholder="结施-101" /></Field>
            <Field label="材料 *">
              <select value={f.materialId} onChange={(e) => {
                const m = meta.materials.find((x) => x.id === Number(e.target.value))
                setF({ ...f, materialId: e.target.value, budgetPrice: m ? '' : f.budgetPrice })
              }}>
                <option value="">请选择</option>
                {meta.materials.map((m) => <option key={m.id} value={m.id}>{m.name}（{m.unit}）</option>)}
              </select>
            </Field>
            <Field label="归口班组">
              <select value={f.crewId} onChange={(e) => setF({ ...f, crewId: e.target.value })}>
                <option value="">跨班组共用</option>
                {meta.crews.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="预算量 *"><input type="number" value={f.budgetQty} onChange={(e) => setF({ ...f, budgetQty: e.target.value })} /></Field>
            <Field label="预算单价"><input type="number" value={f.budgetPrice} onChange={(e) => setF({ ...f, budgetPrice: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
    </Card>
  )
}
