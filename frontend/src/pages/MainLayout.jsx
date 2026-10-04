import { Layout, Menu, Typography, Tag, Dropdown, Button } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { ROLES } from '../dicts.js'

const { Header, Sider, Content } = Layout

export default function MainLayout({ user, menus }) {
  const nav = useNavigate()
  const loc = useLocation()
  const visible = menus.map((k) => {
    const m = MENU_ITEMS[k]
    return { key: m.path, icon: <span>{m.icon}</span>, label: m.label }
  })
  const selected = '/' + loc.pathname.split('/')[1]

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    nav('/login')
    location.reload()
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 20px', background: '#0b2a5b'
      }}>
        <div style={{ color: '#fff', fontWeight: 700, fontSize: 17 }}>
          🏗️ 工地材料领用退库损耗与班组核算平台
        </div>
        <div style={{ color: '#fff' }}>
          <Tag color="gold">{ROLES[user.role] || user.role}</Tag>
          <span style={{ marginRight: 12 }}>{user.realName}</span>
          <Button size="small" ghost onClick={logout}>退出</Button>
        </div>
      </Header>
      <Layout>
        <Sider width={208} theme="light" breakpoint="lg" collapsedWidth={0}>
          <Menu
            mode="inline"
            style={{ height: '100%', borderRight: 0, paddingTop: 8 }}
            selectedKeys={[selected]}
            items={visible}
            onClick={({ key }) => nav(key)}
          />
        </Sider>
        <Content style={{ padding: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

import { MENU_META } from '../menus.js'
const MENU_ITEMS = Object.fromEntries(MENU_META.map((m) => [m.key, m]))
