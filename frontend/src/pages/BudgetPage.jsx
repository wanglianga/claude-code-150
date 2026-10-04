import { useMemo, useState } from 'react'
import { Card, Table, Button, Space, Modal, Form, Select, InputNumber, Progress, Tag, App } from 'antd'
import api from '../api.js'
import { useMasters } from '../useMasters.js'

export default function BudgetPage() {
  const { message } = App.useApp()
  const m = useMasters()
  const [buildingId, setBuildingId] = useState()
  const [processId, setProcessId] = useState()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const rows = useMemo(() => {
    return m.budgets
      .filter((b) => !buildingId || b.buildingId === buildingId)
      .filter((b) => !processId || b.processId === processId)
      .map((b) => ({
        ...b,
        buildingName: m.buildings.find((x) => x.id === b.buildingId)?.name,
        processName: m.processes.find((x) => x.id === b.processId)?.name,
        materialName: m.materials.find((x) => x.id === b.materialId)?.name,
        unit: m.materials.find((x) => x.id === b.materialId)?.unit
      }))
  }, [m, buildingId, processId])

  const submit = async () => {
    const v = await form.validateFields()
    await api.post('/masters/budgets', v)
    message.success('预算已保存')
    setOpen(false)
    form.resetFields()
    await m.reload()
  }

  const columns = [
    { title: '楼栋', dataIndex: 'buildingName', width: 120 },
    { title: '工序', dataIndex: 'processName', width: 120 },
    { title: '材料', dataIndex: 'materialName' },
    { title: '单位', dataIndex: 'unit', width: 70 },
    { title: '预算量', dataIndex: 'budgetQty', width: 110, align: 'right' },
    {
      title: '形象进度', dataIndex: 'progressPercent', width: 220,
      render: (v) => {
        const pct = Number(v || 100)
        const color = pct >= 100 ? '#52c41a' : pct >= 50 ? '#1677ff' : '#faad14'
        return (
          <Space>
            <Progress percent={pct} size="small" strokeColor={color} style={{ width: 140 }} />
            <Tag>{pct}%</Tag>
          </Space>
        )
      }
    },
    {
      title: '操作', width: 90,
      render: (_, r) => (
        <Button size="small" type="link" onClick={() => {
          form.setFieldsValue({
            buildingId: r.buildingId, processId: r.processId, materialId: r.materialId,
            budgetQty: Number(r.budgetQty), progressPercent: Number(r.progressPercent || 100)
          })
          setOpen(true)
        }}>调整</Button>
      )
    }
  ]

  return (
    <Card title="预算量与形象进度（采购反推与发料核对的基准）"
      extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>＋ 编制/调整预算</Button>}>
      <Space style={{ marginBottom: 12 }}>
        <Select allowClear placeholder="按楼栋筛选" style={{ width: 180 }} value={buildingId} onChange={setBuildingId}
          options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
        <Select allowClear placeholder="按工序筛选" style={{ width: 180 }} value={processId} onChange={setProcessId}
          options={m.processes.map((p) => ({ value: p.id, label: p.name }))} />
      </Space>
      <Table rowKey="id" loading={m.loading} columns={columns} dataSource={rows}
        size="small" pagination={{ pageSize: 20 }} />

      <Modal title="编制 / 调整预算" open={open} onOk={submit} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical" initialValues={{ progressPercent: 100 }}>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="buildingId" label="楼栋" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
            </Form.Item>
            <Form.Item name="processId" label="工序" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={m.processes.map((p) => ({ value: p.id, label: p.name }))} />
            </Form.Item>
          </Space>
          <Form.Item name="materialId" label="材料" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label"
              options={m.materials.map((x) => ({ value: x.id, label: `${x.name}（${x.unit}）` }))} />
          </Form.Item>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="budgetQty" label="预算总量" style={{ flex: 1 }} rules={[{ required: true }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="progressPercent" label="当前形象进度 %" style={{ flex: 1 }}
              tooltip="采购反推按：实际净耗 ÷ (预算×进度%) 得到实际单耗系数，再反推完工总需求">
              <InputNumber min={0} max={100} precision={1} style={{ width: '100%' }} addonAfter="%" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </Card>
  )
}
