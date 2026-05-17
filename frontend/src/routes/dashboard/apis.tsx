import { FormEvent, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { type RegisteredApi, request } from '~/lib/api'
import { useDashboard } from '~/lib/dashboard-context'
import { useToast } from '~/lib/toast'

export const Route = createFileRoute('/dashboard/apis')({
  component: ApisPage,
})

function ApisPage() {
  const { token, apis, plansByApi, loading, reload } = useDashboard()
  const { toast } = useToast()
  const [apiName, setApiName] = useState('')
  const [targetUrl, setTargetUrl] = useState('')
  const [proxyPath, setProxyPath] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function createApi(e: FormEvent) {
    e.preventDefault()
    if (!token) return
    setSubmitting(true)
    try {
      await request<RegisteredApi>('/api/v1/apis', {
        method: 'POST',
        body: JSON.stringify({ name: apiName, targetUrl, proxyPath }),
      }, token)
      setApiName(''); setTargetUrl(''); setProxyPath('')
      await reload()
      toast('API registered successfully', 'success')
    } catch (e) { toast((e as Error).message, 'error') }
    finally { setSubmitting(false) }
  }

  return (
    <>
      <div className="page-header">
        <h1>Registered APIs</h1>
        <p>Proxy your backend services through the APIGuard gateway. Each API gets a <code>/proxy/&#123;path&#125;</code> route.</p>
      </div>

      {/* How it works callout */}
      <div style={{
        background: 'rgba(79,142,255,0.07)',
        border: '1px solid rgba(79,142,255,0.2)',
        borderRadius: 12,
        padding: '14px 18px',
        marginBottom: 24,
        fontSize: '0.87rem',
        color: 'var(--text-2)',
        display: 'flex',
        gap: 12,
        alignItems: 'flex-start',
      }}>
        <span style={{ fontSize: '1.1rem', flexShrink: 0 }}>💡</span>
        <div>
          <strong style={{ color: 'var(--accent)' }}>How APIs work:</strong> When you register an API with a proxy path (e.g. <code>payments-api</code>),
          all requests to <code>http://gateway:8080/proxy/payments-api/**</code> will be forwarded to your Target URL.
          Consumers must pass a valid API key in the <code>X-API-Key</code> header.
        </div>
      </div>

      <div className="grid-2" style={{ alignItems: 'start' }}>
        {/* Register Form */}
        <div className="card">
          <div className="card-header"><span className="card-title">⚡ Register new API</span></div>
          <div className="card-body">
            <form className="form-grid" onSubmit={createApi}>
              <div className="form-group">
                <label htmlFor="api-name">API Name</label>
                <input id="api-name" value={apiName} required
                  onChange={e => setApiName(e.target.value)}
                  placeholder="Payments API" />
              </div>
              <div className="form-group">
                <label htmlFor="api-target">Target URL <span className="text-muted">(your backend)</span></label>
                <input id="api-target" value={targetUrl} required
                  onChange={e => setTargetUrl(e.target.value)}
                  placeholder="https://backend.company.com" />
              </div>
              <div className="form-group">
                <label htmlFor="api-path">Proxy Path <span className="text-muted">(lowercase, hyphens only)</span></label>
                <input id="api-path" value={proxyPath} required
                  onChange={e => setProxyPath(e.target.value)}
                  placeholder="payments-api" />
                {proxyPath && (
                  <div className="text-sm text-muted mt-1">
                    Gateway URL: <code>http://gateway:8080/proxy/{proxyPath}/**</code>
                  </div>
                )}
              </div>
              <button className="btn btn-primary" type="submit" disabled={submitting || loading}>
                {submitting ? <><span className="spinner" /> Registering…</> : '⚡ Register API'}
              </button>
            </form>
          </div>
        </div>

        {/* APIs Table */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">All APIs</span>
            <span className="badge badge-blue">{apis.length}</span>
          </div>
          {apis.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">⚡</div>
              <div className="empty-title">No APIs registered yet</div>
              <div className="empty-desc">Use the form to register your first API</div>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Name</th><th>Proxy Path</th><th>Target</th><th>Plans</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {apis.map(api => (
                    <tr key={api.id}>
                      <td>{api.name}</td>
                      <td><code>/{api.proxyPath}</code></td>
                      <td className="text-muted text-sm" style={{ maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {api.targetUrl}
                      </td>
                      <td>
                        <Link to="/dashboard/plans" className="badge badge-purple" style={{ textDecoration: 'none' }}>
                          {(plansByApi[api.id] ?? []).length} plans
                        </Link>
                      </td>
                      <td><span className={`badge ${api.active ? 'badge-green' : 'badge-red'}`}>{api.active ? 'Active' : 'Inactive'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Next step hint */}
      {apis.length > 0 && (
        <div style={{ marginTop: 20, textAlign: 'center' }}>
          <span className="text-muted text-sm">Next: </span>
          <Link to="/dashboard/plans" className="btn btn-secondary btn-sm" style={{ marginLeft: 8 }}>
            Create rate limit plans →
          </Link>
        </div>
      )}
    </>
  )
}
