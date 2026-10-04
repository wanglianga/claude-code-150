import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Tag, Space, Modal, Form, Select, InputNumber, Input, App, DatePicker
} from 'antd'
import dayjs from 'dayjs'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { LOSS_REASON, RESPONSIBILITY, money } from '../dicts.js'

const REASON_COLOR = {
  DESIGN_CHANGE: 'purple',
  CONSTRUCTION_ERROR: 'red',
  POOR_STORAGE: 'orange',
  SUPPLIER_QUALITY: 'volcano',
  NORMAL: 'default'
}

export default function LossPage({ user }) {
  const { message } = App.useApp()
  const m = useMasters()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [editForm, setEditForm] = useState({ lossReason: '', responsibility: '', supplierId: null, remark: '' })
  const [reasonFilter, setReasonFilter] = useState()
  const [respFilter, setRespFilter] = useState()
  const [form] = Form.useForm()
  const isCrew = user.role === 'CREW_LEADER'
  const canEdit = ['WAREHOUSE', 'FINANCE', 'ADMIN'].includes(user.role)

  const load = async () => {
    setLoading(true)
    try {
      const params = {}
      if (reasonFilter) params.lossReason = reasonFilter
      if (respFilter) params.responsibility = respFilter
      let data = await api.get('/losses', { params })
      if (isCrew) data = data.filter((r) => r.crewId === user.crewId)
      setRows(data)
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [reasonFilter, respFilter])

  const submit = async () => {
    const v = await form.validateFields()
    await api.post('/losses', {
      materialId: v.materialId,
      batchId: v.batchId,
      crewId: isCrew ? user.crewId : v.crewId,
      buildingId: v.buildingId,
      processId: v.processId,
      qty: v.qty,
      lossReason: v.lossReason,
      responsibility: v.responsibility,
      supplierId: v.responsibility === 'SUPPLIER' ? v.supplierId : undefined,
      stockAdjusted: v.stockAdjusted,
      lossDate: (v.lossDate || dayjs()).format('YYYY-MM-DD'),
      remark: v.remark
    })
    message.success('损耗已登记' + (v.stockAdjusted ? '，仓库库存已调减' : '（现场损耗，不动仓库库存）'))
    setOpen(false)
    form.resetFields()
    load()
  }

  const saveEdit = async () => {
    await api.put(`/losses/${editTarget.id}`, editForm)
    message.success('责任归属/原因已修正，将影响本月核算口径')
    setEditTarget(null)
    load()
  }

  const columns = [
    { title: '损耗单号', dataIndex: 'lossNo', width: 175 },
    { title: '日期', dataIndex: 'lossDate', width: 110 },
    { title: '材料', dataIndex: 'materialName' },
    { title: '数量', dataIndex: 'qty', width: 90, align: 'right', render: (v, r) => `${v} ${r.unit}` },
    { title: '金额', dataIndex: 'amount', width: 100, align: 'right', render: money },
    { title: '损耗原因', dataIndex: 'lossReason', width: 110,
      render: (v) => <Tag color={REASON_COLOR[v]}>{LOSS_REASON[v]}</Tag> },
    { title: '责任归属', dataIndex: 'responsibility', width: 100,
      render: (v) => {
        const color = v === 'CREW' ? 'red' : v === 'SUPPLIER' ? 'orange' : 'blue'
        return <Tag color={color}>{RESPONSIBILITY[v]?.replace(/（.*\)/, '')}</Tag>
      } },
    { title: '班组', dataIndex: 'crewName', width: 110 },
    { title: '供应商', dataIndex: 'supplierName', width: 150 },
    { title: '楼栋/工序', render: (_, r) => `${r.buildingName || '—'} / ${r.processName || '—'}`, width: 160 },
    { title: '说明', dataIndex: 'remark', ellipsis: true },
    { title: '操作', width: 80, fixed: 'right',
      render: (_, r) => canEdit
        ? <Button size="small" type="link" onClick={() => {
            setEditTarget(r)
            setEditForm({ lossReason: r.lossReason, responsibility: r.responsibility,
              supplierId: r.supplierId, remark: r.remark })
          }}>修正归属</Button> : null }
  ]

  const materialId = Form.useWatch('materialId', form)
  const [batches, setBatches] = useState([])
  useEffect(() => {
    if (!materialId) { setBatches([]); return }
    api.get('/inventory/batches', { params: { materialId } })
      .then((rows) => setBatches(rows))
      .catch(() => setBatches([]))
  }, [materialId])

  return (
    <Card title="损耗登记（瓷砖破损 / 砂浆过期 / 设计变更 / 供应商质量）"
      extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>＋ 登记损耗</Button>}>
      <Space style={{ marginBottom: 12 }}>
        <Select allowClear placeholder="损耗原因" style={{ width: 160 }} value={reasonFilter} onChange={setReasonFilter}
          options={Object.entries(LOSS_REASON).map(([k, v]) => ({ value: k, label: v }))} />
        <Select allowClear placeholder="责任归属" style={{ width: 160 }} value={respFilter} onChange={setRespFilter}
          options={Object.entries(RESPONSIBILITY).map(([k, v]) => ({ value: k, label: v.replace(/（.*\)/, '') }))} />
      </Space>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
        size="small" scroll={{ x: 1500 }} pagination={{ pageSize: 15 }} />

      <Modal title="损耗登记" open={open} onOk={submit} onCancel={() => setOpen(false)} width={700} destroyOnClose>
        <Form form={form} layout="vertical"
          initialValues={{ lossReason: 'NORMAL', responsibility: 'PROJECT', stockAdjusted: false,
            crewId: isCrew ? user.crewId : undefined, lossDate: dayjs() }}>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="materialId" label="材料" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label"
                options={m.materials.map((x) => ({ value: x.id, label: `${x.name}（${x.unit}）` }))} />
            </Form.Item>
            <Form.Item name="batchId" label="批次（可空）" style={{ flex: 1 }}>
              <Select allowClear showSearch optionFilterProp="label"
                options={batches.map((b) => ({ value: b.id, label: `${b.batchNo}（在账 ${b.qtyOnHand}，${b.qcStatus === 'PASSED' ? '合格' : b.qcStatus === 'PENDING' ? '待检' : '不合格'}）` }))} />
            </Form.Item>
            <Form.Item name="qty" label="损耗数量" style={{ width: 150 }} rules={[{ required: true }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="lossDate" label="日期" style={{ width: 160 }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }}>
            {!isCrew && (
              <Form.Item name="crewId" label="责任班组" style={{ flex: 1 }}>
                <Select allowClear options={m.crews.map((c) => ({ value: c.id, label: c.name }))} />
              </Form.Item>
            )}
            <Form.Item name="buildingId" label="楼栋" style={{ flex: 1 }}>
              <Select allowClear options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
            </Form.Item>
            <Form.Item name="processId" label="工序" style={{ flex: 1 }}>
              <Select allowClear options={m.processes.map((p) => ({ value: p.id, label: p.name }))} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="lossReason" label="损耗原因" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={Object.entries(LOSS_REASON).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
            <Form.Item name="responsibility" label="责任承担" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={Object.entries(RESPONSIBILITY).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
            <Form.Item shouldUpdate noStyle>
              {() => form.getFieldValue('responsibility') === 'SUPPLIER' ? (
                <Form.Item name="supplierId" label="供应商" style={{ flex: 1 }} rules={[{ required: true, message: '选择供应商' }]}>
                  <Select options={m.suppliers.map((s) => ({ value: s.id, label: s.name }))} />
                </Form.Item>
              ) : <span style={{ flex: 1 }} />}
            </Form.Item>
          </Space>
          <Form.Item name="stockAdjusted"
            label="是否调减仓库批次库存"
            extra="仓内损耗/盘点盘亏勾选；材料已发料到现场后的损耗不勾选（只计责任成本，不动仓库库存）">
            <Select
              options={[{ value: false, label: '否：现场损耗（不扣仓库库存）' }, { value: true, label: '是：仓内损耗/盘亏（调减库存）' }]} />
          </Form.Item>
          <Form.Item name="remark" label="情况说明">
            <Input.TextArea rows={3} placeholder="如：铺贴切割失误及搬运破损10箱；仓库堆放超适用期结块3吨；抽检壁厚不达标退场索赔等" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={`修正损耗归属：${editTarget?.lossNo || ''}`} open={!!editTarget}
        onOk={saveEdit} onCancel={() => setEditTarget(null)} destroyOnClose>
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>{editTarget?.materialName} {editTarget?.qty} {editTarget?.unit}，金额 {editTarget ? money(editTarget.amount) : ''} 元</div>
          <Select style={{ width: '100%' }} value={editForm.lossReason}
            onChange={(v) => setEditForm((p) => ({ ...p, lossReason: v }))}
            options={Object.entries(LOSS_REASON).map(([k, v]) => ({ value: k, label: v }))}
            placeholder="损耗原因" />
          <Select style={{ width: '100%' }} value={editForm.responsibility}
            onChange={(v) => setEditForm((p) => ({ ...p, responsibility: v }))}
            options={Object.entries(RESPONSIBILITY).map(([k, v]) => ({ value: k, label: v }))}
            placeholder="责任承担" />
          {editForm.responsibility === 'SUPPLIER' && (
            <Select style={{ width: '100%' }} value={editForm.supplierId}
              onChange={(v) => setEditForm((p) => ({ ...p, supplierId: v }))}
              options={m.suppliers.map((s) => ({ value: s.id, label: s.name }))}
              placeholder="索赔供应商" />
          )}
          <Input.TextArea rows={3} value={editForm.remark}
            onChange={(e) => setEditForm((p) => ({ ...p, remark: e.target.value }))} />
        </Space>
      </Modal>
    </Card>
  )
}
