import { useEffect, useState } from 'react'
import { Card, DatePicker, Table, Row, Col, Statistic, Tabs, Tag, Empty } from 'antd'
import dayjs from 'dayjs'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from 'recharts'
import api from '../api.js'
import { money } from '../dicts.js'

export default function FinancePage() {
  const [period, setPeriod] = useState(dayjs().format('YYYY-MM'))
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = async (p = period) => {
    setLoading(true)
    try { setData(await api.get('/analytics/finance', { params: { period: p } })) }
    finally { setLoading(false) }
  }
  useEffect(() => { load(period) }, [period])

  const s = data?.summary
  const supplierChart = (data?.supplierSettlement || []).map((r) => ({
    name: r.supplierName.slice(0, 8),
    入库: Number(r.inboundAmount),
    索赔: Number(r.qualityClaim)
  }))
  const crewChart = (data?.crewDeduction || []).map((r) => ({
    name: r.crewName.replace('班组', ''),
    扣款: Number(r.totalDeduction)
  }))

  const supplierCols = [
    { title: '供应商', dataIndex: 'supplierName' },
    { title: '入库金额（结算依据）', dataIndex: 'inboundAmount', align: 'right', render: money },
    { title: '质量索赔扣减', dataIndex: 'qualityClaim', align: 'right',
      render: (v) => Number(v) > 0 ? <span className="positive">-{money(v)}</span> : money(0) },
    { title: '应付金额', dataIndex: 'payableAmount', align: 'right',
      render: (v) => <b>{money(v)}</b> }
  ]
  const crewCols = [
    { title: '班组', dataIndex: 'crewName' },
    { title: '材料成本（实领-退库）', dataIndex: 'materialCost', align: 'right', render: money },
    { title: '责任损耗扣款', dataIndex: 'crewLossDeduction', align: 'right',
      render: (v) => Number(v) > 0 ? <span className="positive">{money(v)}</span> : money(0) },
    { title: '扣款合计', dataIndex: 'totalDeduction', align: 'right', render: (v) => <b>{money(v)}</b> }
  ]
  const changeCols = [
    { title: '签证号', dataIndex: 'changeNo', width: 160 },
    { title: '楼栋', dataIndex: 'buildingName', width: 110 },
    { title: '类型', dataIndex: 'type', width: 100,
      render: (v) => <Tag>{v === 'DESIGN' ? '设计变更' : '施工变更'}</Tag> },
    { title: '材料', dataIndex: 'materialName', width: 180 },
    { title: '增减量', dataIndex: 'changeQty', width: 90, align: 'right' },
    { title: '索赔金额', dataIndex: 'claimAmount', align: 'right',
      render: (v) => <b className="positive">{money(v)}</b> },
    { title: '说明', dataIndex: 'description', ellipsis: true }
  ]

  return (
    <Card title={`财务结算（${period}）：供应商结算 / 班组扣款 / 变更索赔`}
      extra={<DatePicker picker="month" value={dayjs(period)}
        onChange={(d) => d && setPeriod(d.format('YYYY-MM'))} />}>
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={5}><Card size="small"><Statistic title="供应商入库总额" loading={loading}
          value={Number(s?.inboundTotal || 0)} precision={2} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="供应商质量索赔" loading={loading}
          value={Number(s?.qualityClaimTotal || 0)} precision={2} valueStyle={{ color: '#cf1322' }} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="应付供应商净额" loading={loading}
          value={Number(s?.payableTotal || 0)} precision={2} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="班组扣款合计" loading={loading}
          value={Number(s?.crewDeductionTotal || 0)} precision={2} valueStyle={{ color: '#cf1322' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="变更索赔合计" loading={loading}
          value={Number(s?.changeClaimTotal || 0)} precision={2} valueStyle={{ color: '#d46b08' }} /></Card></Col>
      </Row>

      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={14}>
          <Card size="small" title="供应商入库与质量索赔">
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={supplierChart}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" fontSize={11} />
                <YAxis />
                <Tooltip formatter={(v) => money(v)} />
                <Bar dataKey="入库" fill="#1677ff" />
                <Bar dataKey="索赔" fill="#cf1322" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
        <Col span={10}>
          <Card size="small" title="班组扣款">
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={crewChart} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" />
                <YAxis type="category" dataKey="name" width={70} fontSize={11} />
                <Tooltip formatter={(v) => money(v)} />
                <Bar dataKey="扣款" fill="#fa8c16">
                  {crewChart.map((_, i) => <Cell key={i} fill="#fa8c16" />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      <Tabs
        items={[
          {
            key: 'supplier',
            label: `供应商结算（${data?.supplierSettlement?.length || 0}）`,
            children: <Table rowKey="supplierId" loading={loading} size="small"
              columns={supplierCols} dataSource={data?.supplierSettlement || []}
              pagination={false} />
          },
          {
            key: 'crew',
            label: `班组扣款（${data?.crewDeduction?.length || 0}）`,
            children: <Table rowKey="crewId" loading={loading} size="small"
              columns={crewCols} dataSource={data?.crewDeduction || []} pagination={false} />
          },
          {
            key: 'change',
            label: `已批准变更索赔（${data?.changeClaims?.length || 0}）`,
            children: data?.changeClaims?.length
              ? <Table rowKey="changeNo" loading={loading} size="small"
                  columns={changeCols} dataSource={data.changeClaims} pagination={false} />
              : <Empty description="本月暂无已批准变更签证" />
          }
        ]} />
    </Card>
  )
}
