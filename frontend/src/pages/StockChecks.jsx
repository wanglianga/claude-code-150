import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Card, Modal, Field, Badge, fmt, fmtTime, useToast, useMeta, RESPONSIBILITY } from '../ui.jsx'

export default function StockChecks() {
  const toast = useToast()
  const { meta } = useMeta()
  const [rows, setRows] = useState([])
  const [show, setShow] = useState(false)
  const [detail, setDetail] = useState(null)
  const [f, setF] = useState({ scope: '现场仓库', remark: '', items: [{ materialId: '', actualQty: '', responsibility: 'COMPANY', remark: '' }] })

  const load = () => api.get('/api/stock-checks').then(setRows)
  useEffect(() => { load() }, [])

  const submit = async () => {
    if (f.items.some((i) => !i.materialId || i.actualQty === '')) return toast('请完善盘点材料与实盘数量', 'err')
    try {
      await api.post('/api/stock-checks', {
        scope: f.scope, remark: f.remark,
        items: f.items.map((i) => ({
          materialId: Number(i.materialId), actualQty: Number(i.actualQty),
          responsibility: i.responsibility, remark: i.remark
        }))
      })
      toast('盘点单已登记'); setShow(false); load()
    } catch (e) { toast(e.message, 'err') }
  }

  const adjust = async (sc) => {
    if (!window.confirm(`确认按实盘数量调整 ${sc.code} 的批次账面库存？盘亏责任将进入核算口径。`)) return
    try {
      await api.put(`/api/stock-checks/${sc.id}/adjust`, {})
      toast('调账完成，账面库存已更新'); setDetail(null); load()
    } catch (e) { toast(e.message, 'err') }
  }

  return (
    <Card title="盘点与账实调整" hint="登记账面/实盘差异并调账；盘盈盘亏按责任归属进入复盘与核算"
          actions={<button className="btn primary" onClick={() => setShow(true)}>＋ 新建盘点</button>} flush>
      <div className="table-scroll">
        <table className="tbl">
          <thead><tr>
            <th>盘点单号</th><th>盘点范围</th><th>盘点项</th>
            <th className="num">盘盈/盘亏金额</th><th>状态</th>
            <th>盘点人/调账人</th><th>备注</th><th>操作</th>
          </tr></thead>
          <tbody>
          {rows.map((sc) => (
            <tr key={sc.id}>
              <td className="code-link" onClick={() => setDetail(sc)}>{sc.code}</td>
              <td>{sc.scope}</td>
              <td>{sc.items.length} 项</td>
              <td className={`num ${sc.diffTotal < 0 ? 'pos' : 'neg'}`}>{fmt(sc.diffTotal)}</td>
              <td><Badge status={sc.status} /></td>
              <td style={{ fontSize: 12 }}>{sc.recorder}<div className="muted">{sc.confirmer || '—'}</div></td>
              <td className="desc-cell">{sc.remark}</td>
              <td>
                {sc.status === 'COUNTING'
                  ? <button className="btn sm primary" onClick={() => adjust(sc)}>确认调账</button>
                  : <button className="btn sm ghost" onClick={() => setDetail(sc)}>查看</button>}
              </td>
            </tr>
          ))}
          </tbody>
        </table>
      </div>

      {show && (
        <Modal title="新建盘点单" wide onClose={() => setShow(false)}
               footer={<><button className="btn" onClick={() => setShow(false)}>取消</button>
                 <button className="btn primary" onClick={submit}>登记盘点（暂不调账）</button></>}>
          <div className="grid2">
            <Field label="盘点范围"><input value={f.scope} onChange={(e) => setF({ ...f, scope: e.target.value })} /></Field>
            <Field label="备注"><input value={f.remark} onChange={(e) => setF({ ...f, remark: e.target.value })} /></Field>
          </div>
          <table className="tbl">
            <thead><tr><th>材料（默认取最早批次）</th><th style={{ width: 130 }}>实盘数量</th>
              <th style={{ width: 150 }}>盘亏责任归属</th><th>备注</th><th style={{ width: 60 }}></th></tr></thead>
            <tbody>
            {f.items.map((it, idx) => {
              const set = (patch) => {
                const items = f.items.slice()
                items[idx] = { ...items[idx], ...patch }
                setF({ ...f, items })
              }
              return (
                <tr key={idx}>
                  <td>
                    <select value={it.materialId} onChange={(e) => set({ materialId: e.target.value })}>
                      <option value="">请选择材料</option>
                      {meta.materials.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                    </select>
                  </td>
                  <td><input type="number" value={it.actualQty} onChange={(e) => set({ actualQty: e.target.value })} /></td>
                  <td>
                    <select value={it.responsibility} onChange={(e) => set({ responsibility: e.target.value })}>
                      {Object.entries(RESPONSIBILITY).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                    </select>
                  </td>
                  <td><input value={it.remark} onChange={(e) => set({ remark: e.target.value })} placeholder="差异原因" /></td>
                  <td>{f.items.length > 1 && <button className="btn sm danger"
                    onClick={() => setF({ ...f, items: f.items.filter((_, i) => i !== idx) })}>删</button>}</td>
                </tr>
              )
            })}
            </tbody>
          </table>
          <button className="btn" style={{ marginTop: 10 }}
            onClick={() => setF({ ...f, items: [...f.items, { materialId: '', actualQty: '', responsibility: 'COMPANY', remark: '' }] })}>
            ＋ 增加盘点项
          </button>
        </Modal>
      )}

      {detail && (
        <Modal title={`盘点单 ${detail.code}`} wide onClose={() => setDetail(null)}
               footer={<><button className="btn" onClick={() => setDetail(null)}>关闭</button>
                 {detail.status === 'COUNTING' && <button className="btn primary" onClick={() => adjust(detail)}>确认调账</button>}</>}>
          <div className="kpi-note" style={{ marginBottom: 8 }}>
            范围：{detail.scope} · 状态：<Badge status={detail.status} /> ·
            差异合计 <b className={detail.diffTotal < 0 ? 'pos' : 'neg'}>{fmt(detail.diffTotal)} 元</b>
          </div>
          <table className="tbl">
            <thead><tr><th>材料</th><th className="num">账面数量</th><th className="num">实盘数量</th>
              <th className="num">差异数量</th><th className="num">差异金额</th><th>责任归属</th><th>备注</th></tr></thead>
            <tbody>
            {detail.items.map((i, k) => (
              <tr key={k}>
                <td>{i.materialName}</td>
                <td className="num">{fmt(i.bookQty)} {i.unit}</td>
                <td className="num">{fmt(i.actualQty)}</td>
                <td className={`num ${i.diffQty < 0 ? 'pos' : 'neg'}`}>{fmt(i.diffQty)}</td>
                <td className={`num ${i.diffAmount < 0 ? 'pos' : 'neg'}`}>{fmt(i.diffAmount)}</td>
                <td><Badge status="DRAFT" label={RESPONSIBILITY[i.responsibility]} tone="gray" /></td>
                <td className="desc-cell">{i.remark}</td>
              </tr>
            ))}
            </tbody>
          </table>
        </Modal>
      )}
    </Card>
  )
}
