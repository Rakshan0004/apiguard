import { createFileRoute, Link } from '@tanstack/react-router'
import { useDashboard } from '~/lib/dashboard-context'

export const Route = createFileRoute('/dashboard/')({
  component: OverviewPage,
})

function OverviewPage() {
  const { apis, plansByApi, keys } = useDashboard()
  const allPlans = Object.values(plansByApi).flat()
  const maxRpm = allPlans.length ? Math.max(...allPlans.map(p => p.rateLimitRpm)) : null

  return (
    <>
      <div className="page-header">
        <h1>Overview</h1>
        <p>Your APIGuard platform at a glance</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card blue">
          <div className="stat-label">Registered APIs</div>
          <div className="stat-value">{apis.length}</div>
          <div className="stat-sub">Active endpoints</div>
        </div>
        <div className="stat-card purple">
          <div className="stat-label">Plans</div>
          <div className="stat-value">{allPlans.length}</div>
          <div className="stat-sub">Rate limit policies</div>
        </div>
        <div className="stat-card green">
          <div className="stat-label">API Keys</div>
          <div className="stat-value">{keys.length}</div>
          <div className="stat-sub">{keys.filter(k => k.active).length} active</div>
        </div>
        <div className="stat-card amber">
          <div className="stat-label">Max RPM</div>
          <div className="stat-value">{maxRpm ?? '—'}</div>
          <div className="stat-sub">Highest plan limit</div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-header">
            <span className="card-title">Recent APIs</span>
            <Link to="/dashboard/apis" className="btn btn-secondary btn-sm">View all →</Link>
          </div>
          {apis.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">⚡</div>
              <div className="empty-title">No APIs registered</div>
              <div className="empty-desc">
                <Link to="/dashboard/apis">Register your first API →</Link>
              </div>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Name</th><th>Path</th><th>Plans</th><th>Status</th></tr></thead>
                <tbody>
                  {apis.slice(0, 5).map(api => (
                    <tr key={api.id}>
                      <td>{api.name}</td>
                      <td><code>/{api.proxyPath}</code></td>
                      <td>{(plansByApi[api.id] ?? []).length}</td>
                      <td><span className={`badge ${api.active ? 'badge-green' : 'badge-red'}`}>{api.active ? 'Active' : 'Inactive'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <span className="card-title">Recent Keys</span>
            <Link to="/dashboard/keys" className="btn btn-secondary btn-sm">View all →</Link>
          </div>
          {keys.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">⊛</div>
              <div className="empty-title">No keys generated</div>
              <div className="empty-desc">
                <Link to="/dashboard/keys">Generate an API key →</Link>
              </div>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Key</th><th>API · Plan</th><th>Status</th></tr></thead>
                <tbody>
                  {keys.slice(0, 5).map(k => (
                    <tr key={k.id}>
                      <td><code>{k.keyPrefix}…</code></td>
                      <td>{k.apiName} · <span className="text-muted">{k.planName}</span></td>
                      <td><span className={`badge ${k.active ? 'badge-green' : 'badge-red'}`}>{k.active ? 'Active' : 'Inactive'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
