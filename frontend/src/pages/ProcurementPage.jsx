import { useEffect, useState } from 'react'
import { Card, DatePicker, Table, Row, Col, Statistic, Tabs, Tag, App } from 'antd'
import dayjs from 'dayjs'
import api from '../api.js'
import { money, num } from '../dicts.js'

export default function ProcurementPage() {
  const { message } = App.useApp()
  const [period, setPeriod] = useState(dayjs().format('YYYY-MM'))
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = async (p = period) => {
    setLoading(true)
    try {
      // 未生成核算时提示
      const d = await api.get('/analytics/procurement', { params: { period: p } })
      setData(d)
    } finally { setLoading(false) }
  }
  useEffect(() => { load(period) }, [period])

  const plan = data?.procurementPlan || []
  const subs = data?.subcontractSettlement || []
  const needBuy = plan.filter((r) => Number(r.suggestPurchaseQty) > 0)
  const totalBuyAmount = needBuy.reduce((s, r) => s + Number(r.suggestPurchaseAmount), 0)

  const planCols = [
    { title: '材料', dataIndex: 'materialName' },
    { title: '单位', dataIndex: 'unit', width: 60 },
    { title: '预算总量', dataIndex: 'budgetQty', width: 90, align: 'right', render: num },
    { title: '形象进度', dataIndex: 'progressPercent', width: 90, align: 'right',
      render: (v) => `${num(v, 1)}%` },
    { title: '本期净耗', dataIndex: 'netConsumedQty', width: 90, align: 'right', render: num },
    {
      title: '实际/预算消耗率', dataIndex: 'consumeRate', width: 120, align: 'right',
      sorter: (a, b) => a.consumeRate - b.consumeRate,
      render: (v) => {
        const n = Number(v)
        const cls = n > 100 ? 'positive' : n < 80 ? 'negative' : ''
        return <span className={cls}>{num(n, 1)}%</span>
      }
    },
    { title: '加权均价', dataIndex: 'avgUnitPrice', width: 90, align: 'right', render: money },
    { title: '合格在账库存', dataIndex: 'onHandQty', width: 110, align: 'right', render: num },
    { title: '反推完工尚需', dataIndex: 'forecastRemainQty', width: 110, align: 'right', render: num },
    {
      title: '建议采购量', dataIndex: 'suggestPurchaseQty', width: 100, align: 'right',
      render: (v) => Number(v) > 0
        ? <Tag color="red" style={{ fontWeight: 700 }}>{num(v)}</Tag>
        : <span className="muted">0</span>
    },
    {
      title: '预估采购金额', dataIndex: 'suggestPurchaseAmount', width: 120, align: 'right',
      render: (v) => Number(v) > 0 ? <b className="positive">{money(v)}</b> : money(0)
    }
  ]

  const subCols = [
    { title: '班组', dataIndex: 'crewName' },
    { title: '预算成本', dataIndex: 'budgetCost', align: 'right', render: money },
    { title: '净耗材料成本（实领-退库）', dataIndex: 'netCost', align: 'right', render: money },
    { title: '班组责任损耗', dataIndex: 'crewLossCost', align: 'right',
      render: (v) => Number(v) > 0 ? <span className="positive">{money(v)}</span> : money(0) },
    {
      title: '建议分包结算额', dataIndex: 'settleAmount', align: 'right',
      render: (v) => <b style={{ fontSize: 15 }}>{money(v)}</b>
    }
  ]

  return (
    <Card title={`采购反推与分包结算（${period}）——以实际消耗反推后续采购计划`}
      extra={
        <span style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <a onClick={() => message.loading('若数据为空，请先在“月度班组核算”页生成当月核算', 3)}>使用说明</a>
          <DatePicker picker="month" value={dayjs(period)}
            onChange={(d) => d && setPeriod(d.format('YYYY-MM'))} />
        </span>
      }>
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={6}><Card size="small"><Statistic title="需补货材料种类" value={needBuy.length} suffix={`/ ${plan.length}`}
          valueStyle={{ color: needBuy.length ? '#cf1322' : '#389e0d' }} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="建议采购总金额" value={totalBuyAmount} precision={2}
          prefix="¥" valueStyle={{ color: '#cf1322' }} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="分包结算班组数" value={subs.length} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="分包结算总额"
          value={subs.reduce((s, r) => s + Number(r.settleAmount), 0)} precision={2} prefix="¥" /></Card></Col>
      </Row>

      <Tabs items={[
        {
          key: 'plan',
          label: `后续采购计划（${needBuy.length} 项缺口）`,
          children: (
            <div>
              <div className="muted" style={{ marginBottom: 8, fontSize: 12 }}>
                算法：实际单耗系数 = 本期净耗 ÷ (预算总量 × 形象进度%)；反推完工总需 = 预算总量 × 系数；
                建议采购 = max(反推完工尚需 − 合格在账库存, 0)。待检/不合格批次不计入可用库存。
              </div>
              <Table rowKey="materialId" loading={loading} size="small" columns={planCols}
                dataSource={plan} pagination={false} scroll={{ x: 1100 }} />
            </div>
          )
        },
        {
          key: 'sub',
          label: `分包结算建议（${subs.length} 个班组）`,
          children: <Table rowKey="crewId" loading={loading} size="small" columns={subCols}
            dataSource={subs} pagination={false} />
        }
      ]} />
    </Card>
  )
}
