import DynamicMap from '@/components/DynamicMap'
import type { MapPolyline } from '@/components/DynamicMap' // Import the new type
import { env } from '@/env'
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
  const [_, setMessages] = useState<BenchmarkMessage[]>([])
  const clientRef = useRef<Client | null>(null)
  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${protocol}://${env.VITE_WS_HOST}${env.VITE_API}/benchmark`,
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

  const examplePolylines: MapPolyline[] = [
    {
      id: 'route1',
      points: [
        [2, 2],
        [10, 2],
        [10, 8],
        [15, 8],
      ],
      stroke: 'green',
      strokeWidth: 0.6,
      type: 'path',
    },
    {
      id: 'blockageA',
      points: [
        [20, 5],
        [20, 15],
        [25, 15],
      ],
      strokeWidth: 0.8,
      type: 'roadblock',
    },
  ]

  return (
    <DynamicMap
      points={[[5, 13]]}
      stations={[
        {
          id: 'a',
          location: {
            x: 5,
            y: 5,
          },
          currentGLP: 100,
        },
      ]}
      trucks={[
        {
          id: 'a',
          location: {
            x: 5,
            y: 10,
          },
          type: 'A',
          currentGLP: 100,
          currentFuel: 100,
        },
      ]}
      orders={[
        {
          id: 'a',
          location: {
            x: 10,
            y: 10,
          },
          GLPrequested: 100,
          GLPDelivered: 0,
          requestedDate: new Date().toISOString(),
          limitTime: new Date().toISOString(),
        },
      ]}
      polylines={examplePolylines}
    />
  )
}
