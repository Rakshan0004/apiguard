import { useEffect } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { getToken } from '~/lib/auth'

export const Route = createFileRoute('/')({
  component: IndexRoute,
})

function IndexRoute() {
  const navigate = useNavigate()

  useEffect(() => {
    const token = getToken()
    void navigate({ to: token ? '/dashboard' : '/login' })
  }, [navigate])

  return null
}
