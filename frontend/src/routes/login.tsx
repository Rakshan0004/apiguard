import { FormEvent, useEffect, useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { type AuthResponse, request } from '~/lib/api'
import { getToken, setToken } from '~/lib/auth'

export const Route = createFileRoute('/login')({
  component: LoginRoute,
})

function LoginRoute() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isRegisterMode, setIsRegisterMode] = useState(false)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (getToken()) {
      void navigate({ to: '/dashboard' })
    }
  }, [navigate])

  async function submitAuth(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setMessage(null)
    setError(null)
    try {
      const endpoint = isRegisterMode ? '/api/v1/auth/register' : '/api/v1/auth/login'
      const data = await request<AuthResponse>(endpoint, {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })

      setToken(data.token)
      localStorage.setItem('apiguard_email', email)
      setPassword('')
      setMessage(isRegisterMode ? 'Registered successfully.' : 'Logged in successfully.')
      void navigate({ to: '/dashboard' })
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-wrap panel">
      <h1>{isRegisterMode ? 'Create APIGuard account' : 'Welcome back to APIGuard'}</h1>
      <p className="subtitle">Manage your APIs, plans, and keys in one console.</p>

      <form className="grid" onSubmit={submitAuth}>
        <div>
          <label>Email</label>
          <input
            type="email"
            value={email}
            required
            onChange={(e) => setEmail(e.target.value)}
            placeholder="owner@company.com"
          />
        </div>
        <div>
          <label>Password</label>
          <input
            type="password"
            value={password}
            minLength={6}
            required
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
        </div>
        <button disabled={loading} type="submit">
          {loading ? 'Please wait...' : isRegisterMode ? 'Register' : 'Login'}
        </button>
        <button className="ghost" type="button" onClick={() => setIsRegisterMode((v) => !v)}>
          {isRegisterMode ? 'Have an account? Login' : 'Need an account? Register'}
        </button>
      </form>

      <p className="subtitle" style={{ marginTop: 12 }}>
        After login, go to <Link to="/dashboard">Dashboard</Link>
      </p>

      {message && <div className="status ok">{message}</div>}
      {error && <div className="status error">{error}</div>}
    </main>
  )
}
