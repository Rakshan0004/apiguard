export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

export type RegisteredApi = {
  id: string
  name: string
  targetUrl: string
  proxyPath: string
  active: boolean
  createdAt?: string
}

export type Plan = {
  id: string
  name: string
  rateLimitRpm: number
  monthlyQuota: number
  webhookEnabled: boolean
  apiId: string
}

export type ApiKeySummary = {
  id: string
  keyPrefix: string
  apiName: string
  planName: string
  active: boolean
  createdAt: string
}

export type AuthResponse = {
  token: string
}

export async function request<T>(
  path: string,
  init: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const text = await res.text()
      if (text) message = text
    } catch {
      // ignore
    }
    throw new Error(message)
  }

  const text = await res.text()
  return (text ? JSON.parse(text) : {}) as T
}
