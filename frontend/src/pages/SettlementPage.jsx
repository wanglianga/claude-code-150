import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Space, Select, DatePicker, App, Statistic, Row, Col, Alert
} from 'antd'
import dayjs from 'dayjs'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { money, num } from '../dicts.js'

const { RangePicker } = DatePicker

export default function SettlementPage() {
  const { message } = App.useApp()
  const m = useMasters()
  const [period, setPeriod] = useState(dayjs().format('YYYY-MM'))
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [buildingId, setBuildingId] = useState()
  const [processId, setProcessId] = useState()
  const [crewId, setCrewId] = useState()

  const load = async (p = period) => {
    setLoading(true)
    try {
      setRows(await api.get('/settlements', {
        params: { period: p, buildingId, processId, crewId }
      }))
    } finally { setLoading(false) }
  }
  useEffect(() => { load(period) }, [buildingId, processId, crewId])

  const generate = async () => {
    const data = await api.post('/settlements/generate', null, { params: { period } })
    setRows(data)
    message.success(`${period} 月度班组材料成本已生成，共 ${data.length} 条明细（重复生成会重算覆盖）`)
  }

  const summary = rows.reduce((acc, r) => {
    acc.budget += Number(r.budgetCost)
    acc.crew += Number(r.crewCost)
    acc.crewLoss += Number(r.crewLossCost)
    acc.supplier += Number(r.supplierClaimCost)
    acc.project += Number(r.projectLossCost)
    return acc
  }, { budget: 0, crew: 0, crewLoss: 0, supplier: 0, project: 0 })

  const columns = [
    { title: '楼栋', dataIndex: 'buildingName', width: 100 },
    { title: '工序', dataIndex: 'processName', width: 100 },
    { title: '班组', dataIndex: 'crewName', width: 110 },
    { title: '材料', dataIndex: 'materialName' },
    {
      title: '数量口径',
      children: [
        { title: '预算量', dataIndex: 'budgetQty', width: 80, align: 'right', render: (v) => num(v) },
        { title: '实领', dataIndex: 'issuedQty', width: 80, align: 'right', render: (v) => num(v) },
        { title: '退库', dataIndex: 'returnedQty', width: 80, align: 'right', render: (v) => num(v) },
        { title: '净耗', dataIndex: 'netQty', width: 80, align: 'right', render: (v) => <b>{num(v)}</b> }
      ]
    },
    {
      title: '成本口径（元）',
      children: [
        { title: '预算成本', dataIndex: 'budgetCost', width: 100, align: 'right', render: money },
        { title: '实领成本', dataIndex: 'issuedCost', width: 100, align: 'right', render: money },
        { title: '退库冲减', dataIndex: 'returnedCost', width: 100, align: 'right',
          render: (v) => v > 0 ? <span className="negative">-{money(v)}</span> : money(0) },
        { title: '班组责任损耗', dataIndex: 'crewLossCost', width: 110, align: 'right',
          render: (v) => v > 0 ? <span className="positive">{money(v)}</span> : money(0) },
        { title: '供应商索赔', dataIndex: 'supplierClaimCost', width: 100, align: 'right',
          render: (v) => v > 0 ? <span style={{ color: '#d46b08' }}>{money(v)}</span>
            : <span className="muted">{money(v)}</span> },
        { title: '项目承担', dataIndex: 'projectLossCost', width: 100, align: 'right', render: money },
        {
          title: '班组材料成本', dataIndex: 'crewCost', width: 120, align: 'right',
          render: (v) => <b>{money(v)}</b>
        },
        {
          title: '偏差(班组-预算)', dataIndex: 'variance', width: 130, align: 'right',
          render: (v) => <span className={Number(v) > 0 ? 'positive' : 'negative'}>
            {Number(v) > 0 ? '+' : ''}{money(v)}
          </span>
        }
      ]
    }
  ]

  return (
    <Card
      title="月度班组材料成本核算（预算/实领/退库/损耗/责任归属）"
      extra={
        <Space>
          <DatePicker picker="month" value={dayjs(period)} onChange={(d) => d && setPeriod(d.format('YYYY-MM'))} />
          <Button onClick={() => load()}>查询</Button>
          <Button type="primary" onClick={generate}>⚙ 生成/重算 {period} 核算</Button>
        </Space>
      }>
      <Alert type="info" showIcon style={{ marginBottom: 12 }}
        message="口径：班组材料成本 = 实领成本 − 可复用退库冲减 + 班组责任损耗；供应商质量损耗进索赔、不扣班组；设计变更与合理损耗由项目承担。" />
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={4}><Card size="small"><Statistic title="预算成本合计" value={summary.budget} precision={2} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="班组材料成本" value={summary.crew} precision={2}
          valueStyle={{ color: '#cf1322' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="班组责任损耗扣款" value={summary.crewLoss} precision={2} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="供应商质量索赔" value={summary.supplier} precision={2} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="项目承担损耗" value={summary.project} precision={2} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="总偏差" value={summary.crew - summary.budget} precision={2}
          valueStyle={{ color: summary.crew - summary.budget > 0 ? '#cf1322' : '#389e0d' }} /></Card></Col>
      </Row>
      <Space style={{ marginBottom: 12 }}>
        <Select allowClear placeholder="楼栋" style={{ width: 150 }} value={buildingId} onChange={setBuildingId}
          options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
        <Select allowClear placeholder="工序" style={{ width: 150 }} value={processId} onChange={setProcessId}
          options={m.processes.map((p) => ({ value: p.id, label: p.name }))} />
        <Select allowClear placeholder="班组" style={{ width: 150 }} value={crewId} onChange={setCrewId}
          options={m.crews.map((c) => ({ value: c.id, label: c.name }))} />
      </Space>
      <Table rowKey="id" loading={loading || m.loading} columns={columns} dataSource={rows}
        size="small" scroll={{ x: 1700 }} pagination={{ pageSize: 20 }} bordered />
    </Card>
  )
}
