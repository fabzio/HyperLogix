import type { Routes } from '@/domain/Routes'
import { env } from '@/env'
import { StartBenchmark } from '@/services/BenchmarkService'
import { Client } from '@stomp/stompjs'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'

type BenchmarkMessage = {
  routes: Routes
  cost: number
}
export const useStartBenchmark = () => {
  const data = useQuery({
    queryKey: ['benchmark'],
    queryFn: StartBenchmark,
    enabled: false,
  })

  return data
}

export const useWatchBenchmark = () => {
  const [routes, setRoutes] = useState<BenchmarkMessage>()
  const clientRef = useRef<Client | null>(null)
  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${protocol}://${env.VITE_WS_HOST}${env.VITE_API}/benchmark`,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('✅ Connected to WebSocket')
        client.subscribe('/topic/benchmark', (message) => {
          try {
            const parsed = JSON.parse(message.body)
            console.log('Received message:', parsed)
            setRoutes(parsed)
          } catch {
            console.error('Error parsing message:', message.body)
          }
        })
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
    }
  }, [])

  return {
    routes,
  }
}
