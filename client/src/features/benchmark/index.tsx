import DynamicMap from '@/components/DynamicMap'
import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
type BenchmarkMessage =
  | {
      run: number
      totalRuns: number
      cost: number
      timeMs: number
    }
  | string

export default function Benchmark() {
  const [messages, setMessages] = useState<BenchmarkMessage[]>([])
  const clientRef = useRef<Client | null>(null)
  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/api/v1/benchmark',
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('✅ Connected to WebSocket')

        client.subscribe('/topic/benchmark', (message) => {
          console.log('📩 Received:', message.body)

          try {
            const parsed = JSON.parse(message.body)
            setMessages((prev) => [...prev, parsed])
          } catch {
            setMessages((prev) => [...prev, message.body])
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
  return <DynamicMap />
}
