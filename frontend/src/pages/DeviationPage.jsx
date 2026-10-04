import { useEffect, useState } from 'react'
import { Card, DatePicker, Table, Space, Select, Row, Col, Statistic } from 'antd'
import dayjs from 'dayjs'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  Cell
} from 'recharts'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { money } from '../dicts.js'

export default function DeviationPage() {
  const m = useMasters()
  const [period, setPeriod] = useState(dayjs().format('YYYY-MM'))
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [buildingId, setBuildingId] = useState()
  const [crewId, setCrewId] = useState()

  const load = async (p = period) => {
    setLoading(true)
    try { setRows(await api.get('/analytics/deviation', { params: { period: p } })) }
    finally { setLoading(false) }
  }
  useEffect(() => { load(period) }, [period])

  const filtered = rows
    .filter((r) => !buildingId || r.buildingId === buildingId)
    .filter((r) => !crewId || r.crewId === crewId)

  const totalBudget = filtered.reduce((s, r) => s + Number(r.budgetCost), 0)
  const totalCost = filtered.reduce((s, r) => s + Number(r.crewCost), 0)
  const overCount = filtered.filter((r) => Number(r.variance) > 0).length

  const chartData = filtered
    .filter((r) => r.crewId !== 0 || Number(r.variance) !== 0)
    .map((r) => ({
      name: `${r.buildingName.replace('住宅楼', '').replace('地下车库', '地库')}·${r.processName.slice(0, 4)}·${r.crewName.replace('班组', '').slice(0, 5)}`,
      预算: Number(r.budgetCost),
      班组成本: Number(r.crewCost),
      偏差: Number(r.variance)
    }))

  const columns = [
    { title: '楼栋', dataIndex: 'buildingName', width: 110 },
    { title: '工序', dataIndex: 'processName', width: 110 },
    { title: '班组', dataIndex: 'crewName', width: 120 },
    { title: '预算成本（元）', dataIndex: 'budgetCost', align: 'right', render: money },
    { title: '实领成本（元）', dataIndex: 'issuedCost', align: 'right', render: money },
    { title: '班组责任损耗', dataIndex: 'crewLossCost', align: 'right',
      render: (v) => <span className={Number(v) > 0 ? 'positive' : ''}>{money(v)}</span> },
    { title: '班组材料成本', dataIndex: 'crewCost', align: 'right', render: (v) => <b>{money(v)}</b> },
    {
      title: '偏差金额', dataIndex: 'variance', align: 'right', width: 120,
      sorter: (a, b) => a.variance - b.variance,
      defaultSortOrder: 'descend',
      render: (v) => <span className={Number(v) > 0 ? 'positive' : 'negative'}>
        {Number(v) > 0 ? '+' : ''}{money(v)}
      </span>
    },
    {
      title: '偏差率', dataIndex: 'varianceRate', width: 100, align: 'right',
      render: (v) => <span className={Number(v) > 0 ? 'positive' : 'negative'}>{v}%</span>
    }
  ]

  return (
    <Card title={`材料偏差分析（${period}）——按楼栋 / 工序 / 班组查看真实材料消耗`}
      extra={<DatePicker picker="month" value={dayjs(period)} onChange={(d) => d && setPeriod(d.format('YYYY-MM'))} />}>
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={6}><Card size="small"><Statistic title="预算成本合计" value={totalBudget} precision={2} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="班组材料成本合计" value={totalCost} precision={2} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="总体偏差" value={totalCost - totalBudget} precision={2}
          valueStyle={{ color: totalCost - totalBudget > 0 ? '#cf1322' : '#389e0d' }} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="超支班组数" value={overCount} suffix={`/ ${filtered.length}`}
          valueStyle={{ color: '#cf1322' }} /></Card></Col>
      </Row>

      <Card size="small" style={{ marginBottom: 12 }} title="预算 vs 班组实际成本（按偏差降序）">
        <ResponsiveContainer width="100%" height={340}>
          <BarChart data={chartData} margin={{ top: 8, right: 16, bottom: 60, left: 8 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" angle={-35} textAnchor="end" interval={0} height={60} fontSize={11} />
            <YAxis />
            <Tooltip formatter={(v) => money(v)} />
            <Legend />
            <Bar dataKey="预算" fill="#91caff" />
            <Bar dataKey="班组成本" fill="#1677ff" />
            <Bar dataKey="偏差" radius={[3, 3, 0, 0]}>
              {chartData.map((d, i) =>
                <Cell key={i} fill={d.偏差 > 0 ? '#cf1322' : '#389e0d'} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </Card>

      <Space style={{ marginBottom: 12 }}>
        <Select allowClear placeholder="按楼栋筛选" style={{ width: 180 }} value={buildingId} onChange={setBuildingId}
          options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
        <Select allowClear placeholder="按班组筛选" style={{ width: 180 }} value={crewId} onChange={setCrewId}
          options={m.crews.map((c) => ({ value: c.id, label: c.name }))} />
      </Space>
      <Table rowKey={(r) => `${r.buildingId}-${r.processId}-${r.crewId}`}
        loading={loading || m.loading} columns={columns} dataSource={filtered}
        size="small" pagination={false} />
    </Card>
  )
}
