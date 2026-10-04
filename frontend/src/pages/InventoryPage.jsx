import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Space, Modal, Form, Select, InputNumber, DatePicker, Input,
  Tag, App
} from 'antd'
import dayjs from 'dayjs'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { QC_STATUS, QC_COLOR, MATERIAL_CATEGORIES, money } from '../dicts.js'

export default function InventoryPage() {
  const { message } = App.useApp()
  const masters = useMasters()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [qcTarget, setQcTarget] = useState(null)
  const [qcResult, setQcResult] = useState('PASSED')
  const [qcNote, setQcNote] = useState('')
  const [form] = Form.useForm()

  const load = async () => {
    setLoading(true)
    try { setRows(await api.get('/inventory/batches')) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const submitInbound = async () => {
    const v = await form.validateFields()
    await api.post('/inventory/batches', {
      materialId: v.materialId,
      supplierId: v.supplierId,
      spec: v.spec,
      qtyIn: v.qtyIn,
      unitPrice: v.unitPrice,
      inboundDate: (v.inboundDate || dayjs()).format('YYYY-MM-DD')
    })
    message.success('入库登记成功（默认待检，质检合格后方可发料）')
    setOpen(false)
    form.resetFields()
    load()
  }

  const submitQc = async () => {
    await api.post(`/inventory/batches/${qcTarget.id}/qc`, { result: qcResult, note: qcNote })
    message.success('质检结论已更新')
    setQcTarget(null)
    setQcNote('')
    load()
  }

  const columns = [
    { title: '批次号', dataIndex: 'batchNo', width: 170 },
    { title: '材料', dataIndex: 'materialName' },
    { title: '品类', dataIndex: 'category', width: 110 },
    { title: '规格', dataIndex: 'spec', width: 120 },
    { title: '供应商', dataIndex: 'supplierName', width: 180 },
    { title: '入库量', dataIndex: 'qtyIn', width: 90, align: 'right' },
    { title: '在账库存', dataIndex: 'qtyOnHand', width: 100, align: 'right',
      render: (v) => <b className="num">{v}</b> },
    { title: '单位', dataIndex: 'unit', width: 60 },
    { title: '单价', dataIndex: 'unitPrice', width: 90, align: 'right',
      render: (v) => money(v) },
    { title: '库存金额', dataIndex: 'stockValue', width: 110, align: 'right',
      render: (v) => money(v) },
    { title: '质检', dataIndex: 'qcStatus', width: 90,
      render: (v) => <Tag color={QC_COLOR[v]}>{QC_STATUS[v]}</Tag> },
    { title: '入库日期', dataIndex: 'inboundDate', width: 110 },
    { title: '质检备注', dataIndex: 'qcNote', ellipsis: true, width: 160 },
    { title: '操作', width: 130, fixed: 'right',
      render: (_, r) => (
        <Button size="small" type="link"
          onClick={() => { setQcTarget(r); setQcResult(r.qcStatus === 'FAILED' ? 'FAILED' : 'PASSED'); setQcNote(r.qcNote || '') }}>
          质检处理
        </Button>
      ) }
  ]

  return (
    <Card
      title="材料入库与库存（批次 / 规格 / 供应商 / 质检 / 单价）"
      extra={<Button type="primary" onClick={() => setOpen(true)}>＋ 采购入库登记</Button>}>
        <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
          scroll={{ x: 1500 }} size="small" pagination={{ pageSize: 20 }} />

      <Modal title="采购入库登记" open={open} onOk={submitInbound} onCancel={() => setOpen(false)}
        destroyOnClose width={560}>
        <Form form={form} layout="vertical" initialValues={{ inboundDate: dayjs() }}>
          <Form.Item name="materialId" label="材料" rules={[{ required: true, message: '选择材料' }]}>
            <Select
              showSearch optionFilterProp="label"
              options={masters.materials.map((m) => ({
                value: m.id, label: `${m.name}（${m.category}/${m.unit}）`
              }))}
              onChange={(mid) => {
                const m = masters.materials.find((x) => x.id === mid)
                if (m) form.setFieldsValue({ spec: m.spec })
              }}
            />
          </Form.Item>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="supplierId" label="供应商" style={{ flex: 1 }}
              rules={[{ required: true, message: '选择供应商' }]}>
              <Select options={masters.suppliers.map((s) => ({ value: s.id, label: s.name }))} />
            </Form.Item>
            <Form.Item name="inboundDate" label="入库日期" style={{ flex: 1 }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="spec" label="规格型号">
            <Input />
          </Form.Item>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="qtyIn" label="入库数量" style={{ flex: 1 }}
              rules={[{ required: true, message: '填写数量' }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="unitPrice" label="单价（元）" style={{ flex: 1 }}
              rules={[{ required: true, message: '填写单价' }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal title={`质检处理：${qcTarget?.batchNo || ''}`} open={!!qcTarget}
        onOk={submitQc} onCancel={() => setQcTarget(null)} destroyOnClose>
        {qcTarget && (
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>{qcTarget.materialName} / {qcTarget.spec}，在账 {qcTarget.qtyOnHand} {qcTarget.unit}</div>
            <Select value={qcResult} onChange={setQcResult} style={{ width: '100%' }}
              options={[
                { value: 'PASSED', label: '合格放行（可发料）' },
                { value: 'FAILED', label: '不合格冻结（禁止发料）' }
              ]} />
            <Input.TextArea rows={3} value={qcNote} onChange={(e) => setQcNote(e.target.value)}
              placeholder="质检说明（复检报告/强力检测等）" />
          </Space>
        )}
      </Modal>
    </Card>
  )
}
