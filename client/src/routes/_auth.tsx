import { env } from '@/env'
import MainLayout from '@/layouts/MainLayout'
import { useSessionStore } from '@/store/session'
import { useWebSocketStore } from '@/store/websocket'
import { Outlet, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'

export const Route = createFileRoute('/_auth')({
  component: RouteComponent,
})

function RouteComponent() {
  const navigate = useNavigate()
  const { username } = useSessionStore()
  const { connect, disconnect } = useWebSocketStore()
  useEffect(() => {
    if (!username) {
      navigate({ to: '/login' })
      return
    }
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    connect(
      `${protocol}://${env.VITE_WS_HOST}${env.VITE_API}/ws?user=${username}`,
    )
    return () => {
      disconnect()
    }
  }, [connect, disconnect, username, navigate])

  return (
    <MainLayout>
      <Outlet />
    </MainLayout>
  )
}
