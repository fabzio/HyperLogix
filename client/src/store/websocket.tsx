import { Client, type StompSubscription } from '@stomp/stompjs'
import { create } from 'zustand'

export interface WebSocketStore {
  client: Client | null
  connected: boolean
  subscriptions: Map<string, StompSubscription>

  connect: (url: string) => Promise<void>
  disconnect: () => Promise<void>

  subscribe: (destination: string, callback: (message: unknown) => void) => void
  unsubscribe: (destination: string) => void
  publish: (
    destination: string,
    body: string,
    headers?: Record<string, string>,
  ) => void
}

export const useWebSocketStore = create<WebSocketStore>((set, get) => ({
  client: null,
  connected: false,
  subscriptions: new Map(),

  connect: async (url: string) => {
    const client = new Client({
      brokerURL: url,
      onConnect: () => {
        set({ connected: true })
        console.log('Connected to STOMP websocket server')
      },
      onDisconnect: () => {
        console.log('Disconnected from STOMP websocket server')
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame)
      },
    })

    await new Promise<void>((resolve, reject) => {
      try {
        client.activate()
        client.onConnect = () => {
          set({ connected: true })
          resolve()
        }
        client.onStompError = (frame) => {
          reject(new Error(`STOMP error: ${frame.headers.message}`))
        }
      } catch (error) {
        reject(error)
      }
    })

    set({ client })
  },

  disconnect: async () => {
    const { client, subscriptions } = get()
    if (client) {
      for (const subscription of subscriptions.values()) {
        subscription.unsubscribe()
      }

      await new Promise<void>((resolve) => {
        client.deactivate()
        resolve()
      })

      set({ client: null, connected: false, subscriptions: new Map() })
    }
  },

  subscribe: (destination: string, callback: (message: unknown) => void) => {
    const { client, subscriptions } = get()
    if (!client || !client.connected) {
      console.error('Cannot subscribe: WebSocket not connected')
      return
    }

    if (subscriptions.has(destination)) {
      console.warn(`Already subscribed to ${destination}, unsubscribing first`)
      get().unsubscribe(destination)
    }

    const subscription = client.subscribe(destination, (message) => {
      const body = message.body
      let parsedBody: unknown
      try {
        parsedBody = JSON.parse(body)
      } catch (e) {
        parsedBody = body
      }
      callback(parsedBody)
    })

    set((state) => {
      const newSubscriptions = new Map(state.subscriptions)
      newSubscriptions.set(destination, subscription)
      return { subscriptions: newSubscriptions }
    })
  },

  unsubscribe: (destination: string) => {
    const { subscriptions } = get()
    const subscription = subscriptions.get(destination)

    if (subscription) {
      subscription.unsubscribe()
      set((state) => {
        const newSubscriptions = new Map(state.subscriptions)
        newSubscriptions.delete(destination)
        return { subscriptions: newSubscriptions }
      })
    }
  },

  publish: (
    destination: string,
    body: string,
    headers?: Record<string, string>,
  ) => {
    const { client } = get()
    if (!client || !client.connected) {
      console.error('Cannot publish: WebSocket not connected')
      return
    }

    client.publish({
      destination,
      body,
      headers,
    })
  },
}))
