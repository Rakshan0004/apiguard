import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from '@tanstack/react-router'
import { DashboardProvider, useDashboard } from '~/lib/dashboard-context'
import { clearToken } from '~/lib/auth'
import { useToast } from '~/lib/toast'
import { API_BASE_URL } from '~/lib/api'

export const Route = createFileRoute('/dashboard')({
  component: DashboardLayout,
})

const navItems = [
  { to: '/dashboard/', icon: '⬡', label: 'Overview' },
  { to: '/dashboard/apis', icon: '⚡', label: 'APIs' },
  { to: '/dashboard/plans', icon: '◈', label: 'Plans' },
  { to: '/dashboard/keys', icon: '⊛', label: 'API Keys' },
]

function Sidebar() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { apis, plansByApi, keys, loading } = useDashboard()
  const routerState = useRouterState()
  const currentPath = routerState.location.pathname

  const userEmail = localStorage.getItem('apiguard_email') ?? 'admin@apiguard.io'
  const userInitials = userEmail.slice(0, 2).toUpperCase()

  const badgeMap: Record<string, number | undefined> = {
    '/dashboard/apis': apis.length || undefined,
    '/dashboard/plans': Object.values(plansByApi).flat().length || undefined,
    '/dashboard/keys': keys.length || undefined,
  }

  function logout() {
    clearToken()
    toast('Signed out successfully', 'info')
    void navigate({ to: '/login' })
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-logo-icon">A</div>
        <span className="sidebar-logo-name">API<span>Guard</span></span>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section-label">Navigation</div>
        {navItems.map(item => {
          const isActive = item.to === '/dashboard/'
            ? currentPath === '/dashboard' || currentPath === '/dashboard/'
            : currentPath.startsWith(item.to)
          return (
            <Link
              key={item.to}
              to={item.to}
              className={`nav-item${isActive ? ' active' : ''}`}
            >
              <span className="nav-item-icon">{item.icon}</span>
              {item.label}
              {badgeMap[item.to] !== undefined && (
                <span className="nav-item-badge">{badgeMap[item.to]}</span>
              )}
            </Link>
          )
        })}

        <div className="nav-section-label" style={{ marginTop: 8 }}>Resources</div>
        <a
          href={API_BASE_URL + '/swagger-ui.html'}
          target="_blank"
          rel="noreferrer"
          className="nav-item"
        >
          <span className="nav-item-icon">📄</span>
          API Docs
        </a>
        <Link to="/" className="nav-item">
          <span className="nav-item-icon">❓</span>
          How it works
        </Link>
      </nav>

      <div className="sidebar-footer">
        <div className="user-card">
          <div className="user-avatar">{userInitials}</div>
          <div className="user-info">
            <div className="user-name" title={userEmail}>{userEmail}</div>
            <div className="user-role">Administrator</div>
          </div>
        </div>
        <button className="btn btn-ghost btn-full btn-sm" style={{ marginTop: 8 }} onClick={logout}>
          Sign out
        </button>
      </div>
    </aside>
  )
}

function TopBar() {
  const { loading } = useDashboard()
  const routerState = useRouterState()
  const path = routerState.location.pathname
  const titleMap: Record<string, string> = {
    '/dashboard': 'Overview',
    '/dashboard/': 'Overview',
    '/dashboard/apis': 'APIs',
    '/dashboard/plans': 'Plans',
    '/dashboard/keys': 'API Keys',
  }
  const title = titleMap[path] ?? 'Dashboard'

  return (
    <header className="topbar">
      <span className="topbar-title">{title}</span>
      <div className="topbar-actions">
        {loading && <span className="spinner" style={{ borderTopColor: 'var(--accent)' }} />}
        <span className="badge badge-green">● Live</span>
      </div>
    </header>
  )
}

function DashboardLayout() {
  return (
    <DashboardProvider>
      <div className="app-shell">
        <Sidebar />
        <div className="main-content">
          <TopBar />
          <div className="page-content animate-fade-up">
            <Outlet />
          </div>
        </div>
      </div>
    </DashboardProvider>
  )
}
