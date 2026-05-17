import { FormEvent, useMemo, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { request } from '~/lib/api'
import { useDashboard } from '~/lib/dashboard-context'
import { useToast } from '~/lib/toast'

export const Route = createFileRoute('/dashboard/keys')({
  component: KeysPage,
})

function KeysPage() {
  const { token, apis, plansByApi, keys, loading, reload } = useDashboard()
  const { toast } = useToast()

  const [keyApiId, setKeyApiId] = useState('')
  const [selectedPlanId, setSelectedPlanId] = useState('')
  const [freshKey, setFreshKey] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [copied, setCopied] = useState(false)

  const keyApiPlans = useMemo(() => plansByApi[keyApiId] ?? [], [plansByApi, keyApiId])
  const allPlansExist = Object.values(plansByApi).flat().length > 0

  async function createKey(e: FormEvent) {
    e.preventDefault()
    if (!token || !keyApiId || !selectedPlanId) return
    setSubmitting(true)
    setFreshKey(null)
    try {
      const created = await request<{ apiKey: string; message: string }>(
        `/api/v1/keys/generate?apiId=${keyApiId}&planId=${selectedPlanId}`,
        { method: 'POST' }, token
      )
      setFreshKey(created.apiKey)
      await reload()
      toast('API key generated — copy it now!', 'success')
    } catch (e) { toast((e as Error).message, 'error') }
    finally { setSubmitting(false) }
  }

  function copyKey() {
    if (!freshKey) return
    navigator.clipboard.writeText(freshKey)
    setCopied(true)
    toast('API key copied to clipboard', 'success')
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <>
      <div className="page-header">
        <h1>API Keys</h1>
        <p>Generate secure API keys for consumers. Keys are tied to a specific API and plan.</p>
      </div>

      <div style={{
        background: 'rgba(52,211,153,0.07)',
        border: '1px solid rgba(52,211,153,0.2)',
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
          <strong style={{ color: 'var(--green)' }}>How API Keys work:</strong> Generate a key and share it with your consumer.
          They must send it as <code>X-API-Key: {'<key>'}</code> in every request to the gateway.
          The gateway validates the key, checks the rate limit plan, and forwards the request to your backend.
          <strong> The full key is only shown once</strong> — store it securely.
        </div>
      </div>

      {!allPlansExist ? (
        <div className="card">
          <div className="empty-state">
            <div className="empty-icon">⊛</div>
            <div className="empty-title">Create a plan first</div>
            <div className="empty-desc">API keys must be linked to a plan which sets the rate limits.</div>
            <Link to="/dashboard/plans" className="btn btn-primary btn-sm" style={{ marginTop: 16, display: 'inline-flex' }}>
              Go to Plans →
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid-2" style={{ alignItems: 'start' }}>
          {/* Generate Form */}
          <div className="card">
            <div className="card-header"><span className="card-title">⊛ Generate new key</span></div>
            <div className="card-body">
              <form className="form-grid" onSubmit={createKey}>
                <div className="form-group">
                  <label htmlFor="key-api">API</label>
                  <select id="key-api" value={keyApiId} required
                    onChange={e => { setKeyApiId(e.target.value); setSelectedPlanId(''); setFreshKey(null) }}>
                    <option value="">Select API…</option>
                    {apis.map(api => <option key={api.id} value={api.id}>{api.name}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="key-plan">Plan</label>
                  <select id="key-plan" value={selectedPlanId} required
                    onChange={e => setSelectedPlanId(e.target.value)}
                    disabled={!keyApiId || keyApiPlans.length === 0}>
                    <option value="">
                      {keyApiId && keyApiPlans.length === 0 ? 'No plans for this API' : 'Select plan…'}
                    </option>
                    {keyApiPlans.map(plan => (
                      <option key={plan.id} value={plan.id}>
                        {plan.name} ({plan.rateLimitRpm} rpm · {plan.monthlyQuota.toLocaleString()} mo)
                      </option>
                    ))}
                  </select>
                </div>
                <button className="btn btn-primary" type="submit"
                  disabled={submitting || loading || !keyApiId || !selectedPlanId}>
                  {submitting ? <><span className="spinner" /> Generating…</> : '⊛ Generate Key'}
                </button>
              </form>

              {/* Fresh Key Reveal */}
              {freshKey && (
                <div className="key-reveal">
                  <div className="key-reveal-label">
                    ✓ New API Key — shown only once, copy immediately
                  </div>
                  <div className="key-value">{freshKey}</div>
                  <button
                    className={`btn btn-sm key-copy-btn ${copied ? 'btn-secondary' : 'btn-primary'}`}
                    onClick={copyKey}
                  >
                    {copied ? '✓ Copied!' : '📋 Copy to clipboard'}
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Keys Table */}
          <div className="card">
            <div className="card-header">
              <span className="card-title">All Keys</span>
              <span className="badge badge-blue">{keys.length}</span>
            </div>
            {keys.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">⊛</div>
                <div className="empty-title">No keys generated yet</div>
                <div className="empty-desc">Use the form to generate your first API key</div>
              </div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>Key Prefix</th><th>API</th><th>Plan</th><th>Created</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {keys.map(k => (
                      <tr key={k.id}>
                        <td><code>{k.keyPrefix}…</code></td>
                        <td>{k.apiName}</td>
                        <td className="text-muted">{k.planName}</td>
                        <td className="text-muted text-sm">{new Date(k.createdAt).toLocaleDateString()}</td>
                        <td><span className={`badge ${k.active ? 'badge-green' : 'badge-red'}`}>{k.active ? 'Active' : 'Inactive'}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  )
}
