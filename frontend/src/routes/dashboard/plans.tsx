import { FormEvent, useMemo, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { type Plan, request } from '~/lib/api'
import { useDashboard } from '~/lib/dashboard-context'
import { useToast } from '~/lib/toast'

export const Route = createFileRoute('/dashboard/plans')({
  component: PlansPage,
})

// Pre-defined templates for quick-fill
const PLAN_TEMPLATES = [
  {
    label: 'Free',
    icon: '🆓',
    color: 'var(--green)',
    name: 'Free',
    rateLimitRpm: 10,
    monthlyQuota: 1000,
    webhookEnabled: false,
    desc: '10 req/min · 1k/mo',
  },
  {
    label: 'Starter',
    icon: '🚀',
    color: 'var(--accent)',
    name: 'Starter',
    rateLimitRpm: 60,
    monthlyQuota: 10000,
    webhookEnabled: false,
    desc: '60 req/min · 10k/mo',
  },
  {
    label: 'Pro',
    icon: '⭐',
    color: 'var(--accent-2)',
    name: 'Pro',
    rateLimitRpm: 300,
    monthlyQuota: 100000,
    webhookEnabled: true,
    desc: '300 req/min · 100k/mo',
  },
  {
    label: 'Enterprise',
    icon: '🏢',
    color: 'var(--amber)',
    name: 'Enterprise',
    rateLimitRpm: 1000,
    monthlyQuota: 1000000,
    webhookEnabled: true,
    desc: '1k req/min · 1M/mo',
  },
]

function PlansPage() {
  const { token, apis, plansByApi, loading, reload } = useDashboard()
  const { toast } = useToast()
  const allPlans = useMemo(() => Object.values(plansByApi).flat(), [plansByApi])

  // Form state
  const [selectedApiId, setSelectedApiId] = useState('')
  const [planName, setPlanName] = useState('')
  const [rateLimitRpm, setRateLimitRpm] = useState(60)
  const [monthlyQuota, setMonthlyQuota] = useState(10000)
  const [webhookEnabled, setWebhookEnabled] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [activeTemplate, setActiveTemplate] = useState<string | null>(null)

  // Confirmation delete state
  const [deletingPlanId, setDeletingPlanId] = useState<string | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)

  function applyTemplate(tpl: typeof PLAN_TEMPLATES[0]) {
    setPlanName(tpl.name)
    setRateLimitRpm(tpl.rateLimitRpm)
    setMonthlyQuota(tpl.monthlyQuota)
    setWebhookEnabled(tpl.webhookEnabled)
    setActiveTemplate(tpl.label)
  }

  function resetForm() {
    setPlanName('')
    setRateLimitRpm(60)
    setMonthlyQuota(10000)
    setWebhookEnabled(false)
    setActiveTemplate(null)
  }

  async function createPlan(e: FormEvent) {
    e.preventDefault()
    if (!token || !selectedApiId) return
    if (rateLimitRpm < 1 || monthlyQuota < 1) {
      toast('Rate limit and quota must be at least 1', 'error')
      return
    }
    // Duplicate name check
    const existing = (plansByApi[selectedApiId] ?? []).find(
      p => p.name.toLowerCase() === planName.toLowerCase()
    )
    if (existing) {
      toast(`A plan named "${planName}" already exists for this API`, 'error')
      return
    }
    setSubmitting(true)
    try {
      await request<Plan>(`/api/v1/apis/${selectedApiId}/plans`, {
        method: 'POST',
        body: JSON.stringify({ name: planName, rateLimitRpm, monthlyQuota, webhookEnabled }),
      }, token)
      resetForm()
      await reload()
      toast('Plan created successfully', 'success')
    } catch (e) { toast((e as Error).message, 'error') }
    finally { setSubmitting(false) }
  }

  return (
    <>
      <div className="page-header">
        <h1>Rate Limit Plans</h1>
        <p>Define how many requests consumers can make per minute and per month for each API.</p>
      </div>

      {/* Info callout */}
      <div style={{
        background: 'rgba(167,139,250,0.07)',
        border: '1px solid rgba(167,139,250,0.2)',
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
          <strong style={{ color: 'var(--accent-2)' }}>How Plans work:</strong>{' '}
          A plan defines the <strong>rate limit (RPM)</strong> — max requests per minute — and the{' '}
          <strong>monthly quota</strong>. When a consumer exceeds either limit, the gateway automatically
          returns <code>429 Too Many Requests</code>. You type a custom plan name (e.g. <em>Starter</em>, <em>Pro</em>)
          or use a template below to auto-fill values.
        </div>
      </div>

      {apis.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <div className="empty-icon">◈</div>
            <div className="empty-title">Register an API first</div>
            <div className="empty-desc">You need at least one API before creating plans.</div>
            <Link to="/dashboard/apis" className="btn btn-primary btn-sm" style={{ marginTop: 16, display: 'inline-flex' }}>
              Go to APIs →
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid-2" style={{ alignItems: 'start' }}>

          {/* ── Create Plan Form ── */}
          <div className="card">
            <div className="card-header"><span className="card-title">◈ Create plan</span></div>
            <div className="card-body">

              {/* Template quick-select */}
              <div style={{ marginBottom: 20 }}>
                <label style={{ marginBottom: 10, display: 'block' }}>
                  Quick templates <span className="text-muted">(optional — click to auto-fill)</span>
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 8 }}>
                  {PLAN_TEMPLATES.map(tpl => (
                    <button
                      key={tpl.label}
                      type="button"
                      onClick={() => applyTemplate(tpl)}
                      style={{
                        padding: '10px 12px',
                        borderRadius: 10,
                        border: `1px solid ${activeTemplate === tpl.label ? tpl.color : 'var(--border)'}`,
                        background: activeTemplate === tpl.label ? `${tpl.color}18` : 'var(--surface-2)',
                        color: activeTemplate === tpl.label ? tpl.color : 'var(--text-2)',
                        cursor: 'pointer',
                        textAlign: 'left',
                        transition: 'all 0.15s',
                        fontFamily: 'inherit',
                      }}
                    >
                      <div style={{ fontWeight: 600, fontSize: '0.85rem', marginBottom: 2 }}>
                        {tpl.icon} {tpl.label}
                      </div>
                      <div style={{ fontSize: '0.75rem', opacity: 0.75 }}>{tpl.desc}</div>
                    </button>
                  ))}
                </div>
              </div>

              <hr className="divider" />

              {/* Form */}
              <form className="form-grid" onSubmit={createPlan}>
                <div className="form-group">
                  <label htmlFor="plan-api">API *</label>
                  <select id="plan-api" value={selectedApiId} required
                    onChange={e => setSelectedApiId(e.target.value)}>
                    <option value="">Select an API…</option>
                    {apis.map(api => <option key={api.id} value={api.id}>{api.name}</option>)}
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="plan-name">
                    Plan Name *{' '}
                    <span className="text-muted">(free text — e.g. Starter, Pro, Enterprise)</span>
                  </label>
                  <input
                    id="plan-name"
                    value={planName}
                    required
                    maxLength={64}
                    onChange={e => { setPlanName(e.target.value); setActiveTemplate(null) }}
                    placeholder="e.g. Starter"
                  />
                </div>

                <div className="grid-2">
                  <div className="form-group">
                    <label htmlFor="plan-rpm">
                      Rate Limit (req/min) *
                    </label>
                    <input
                      id="plan-rpm"
                      type="number"
                      min={1}
                      max={100000}
                      value={rateLimitRpm}
                      required
                      onChange={e => setRateLimitRpm(Number(e.target.value))}
                    />
                    <div className="text-sm text-muted mt-1">
                      Max {rateLimitRpm} requests per minute
                    </div>
                  </div>
                  <div className="form-group">
                    <label htmlFor="plan-quota">Monthly Quota *</label>
                    <input
                      id="plan-quota"
                      type="number"
                      min={1}
                      max={100000000}
                      value={monthlyQuota}
                      required
                      onChange={e => setMonthlyQuota(Number(e.target.value))}
                    />
                    <div className="text-sm text-muted mt-1">
                      {monthlyQuota.toLocaleString()} requests/month
                    </div>
                  </div>
                </div>

                <label className="checkbox-row">
                  <input type="checkbox" checked={webhookEnabled}
                    onChange={e => setWebhookEnabled(e.target.checked)} />
                  Enable webhook notifications on quota events
                </label>

                <div style={{ display: 'flex', gap: 10 }}>
                  <button className="btn btn-primary" type="submit"
                    disabled={submitting || loading || !selectedApiId || !planName.trim()}
                    style={{ flex: 1 }}>
                    {submitting ? <><span className="spinner" /> Creating…</> : '◈ Create Plan'}
                  </button>
                  <button type="button" className="btn btn-ghost" onClick={resetForm}>
                    Reset
                  </button>
                </div>
              </form>
            </div>
          </div>

          {/* ── Plans Table ── */}
          <div className="card">
            <div className="card-header">
              <span className="card-title">All Plans</span>
              <span className="badge badge-purple">{allPlans.length}</span>
            </div>
            {allPlans.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">◈</div>
                <div className="empty-title">No plans yet</div>
                <div className="empty-desc">Use a template or create a custom plan on the left</div>
              </div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Plan</th>
                      <th>API</th>
                      <th>RPM</th>
                      <th>Monthly</th>
                      <th>Webhook</th>
                    </tr>
                  </thead>
                  <tbody>
                    {apis.flatMap(api =>
                      (plansByApi[api.id] ?? []).map(plan => (
                        <tr key={plan.id}>
                          <td>
                            <span style={{ fontWeight: 600 }}>{plan.name}</span>
                          </td>
                          <td className="text-muted text-sm">{api.name}</td>
                          <td>
                            <span className="badge badge-blue">{plan.rateLimitRpm.toLocaleString()}/min</span>
                          </td>
                          <td>
                            <span className="badge badge-purple">{plan.monthlyQuota.toLocaleString()}</span>
                          </td>
                          <td>
                            <span className={`badge ${plan.webhookEnabled ? 'badge-green' : 'badge-amber'}`}>
                              {plan.webhookEnabled ? '● On' : '○ Off'}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {deletingPlanId && (
        <div style={{
          position: 'fixed', inset: 0,
          background: 'rgba(0,0,0,0.7)',
          backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000,
        }}>
          <div className="card" style={{ maxWidth: 400, width: '90%' }}>
            <div className="card-header">
              <span className="card-title" style={{ color: 'var(--red)' }}>⚠ Delete Plan</span>
            </div>
            <div className="card-body">
              <p style={{ color: 'var(--text-2)', marginBottom: 20 }}>
                This will permanently delete the plan. Any API keys using this plan will stop working.
                This action cannot be undone.
              </p>
              <div style={{ display: 'flex', gap: 10 }}>
                <button
                  className="btn btn-danger"
                  style={{ flex: 1 }}
                  disabled={deleteLoading}
                  onClick={async () => {
                    setDeleteLoading(true)
                    try {
                      await request(`/api/v1/plans/${deletingPlanId}`, { method: 'DELETE' }, token ?? '')
                      await reload()
                      toast('Plan deleted', 'info')
                    } catch (e) { toast((e as Error).message, 'error') }
                    finally { setDeleteLoading(false); setDeletingPlanId(null) }
                  }}
                >
                  {deleteLoading ? <><span className="spinner" /> Deleting…</> : 'Yes, delete plan'}
                </button>
                <button className="btn btn-ghost" onClick={() => setDeletingPlanId(null)}>
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {allPlans.length > 0 && (
        <div style={{ marginTop: 20, textAlign: 'center' }}>
          <span className="text-muted text-sm">Next: </span>
          <Link to="/dashboard/keys" className="btn btn-secondary btn-sm" style={{ marginLeft: 8 }}>
            Generate API keys →
          </Link>
        </div>
      )}
    </>
  )
}

export default PlansPage
