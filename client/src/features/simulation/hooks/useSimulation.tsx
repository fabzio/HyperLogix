import type { PLGNetwork } from '@/domain/PLGNetwork'
import {
  getSimulationStatus,
  startSimulation,
  stopSimulation,
} from '@/services/SimulatorService'
import { useSessionStore } from '@/store/session'
import { useWebSocketStore } from '@/store/websocket'
import {
  useMutation,
  useQueryClient,
  useSuspenseQuery,
} from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'

type MesaggeResponse = {
  timestamp: Date
  plgNetwork: PLGNetwork
}

export const useStartSimulation = () => {
  const { username } = useSessionStore()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: {
      endTimeOrders: string
      startTimeOrders: string
    }) => {
      if (!username) {
        throw new Error('Username is required to start simulation')
      }
      return startSimulation({
        endTimeOrders: params.endTimeOrders,
        startTimeOrders: params.startTimeOrders,
        simulationId: username,
      })
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['simulation'] }),
  })
}

export const useWatchSimulation = () => {
  const [network, setNetwork] = useState<PLGNetwork | null>(null)
  const { username } = useSessionStore()
  const { subscribe, unsubscribe, publish, connected, client } =
    useWebSocketStore()

  const handleMessage = useCallback((message: unknown) => {
    try {
      const typedMessage = message as MesaggeResponse
      setNetwork(typedMessage.plgNetwork)
    } catch (error) {
      console.error('Error parsing message:', error)
    }
  }, [])

  useEffect(() => {
    if (!client || !connected) return
    subscribe(`/topic/simulation/${username}`, handleMessage)
    return () => {
      unsubscribe(`/topic/simulation/${username}`)
    }
  }, [subscribe, unsubscribe, connected, client, username, handleMessage])

  return {
    network,
    startSimulation: (params: {
      startTimeOrders: string
      endTimeOrders: string
    }) => {
      publish('/app/simulation/start', JSON.stringify(params))
    },
  }
}

export const useStatusSimulation = () => {
  const { username } = useSessionStore()
  return useSuspenseQuery({
    queryKey: ['simulation'],
    queryFn: () => {
      if (!username) {
        throw new Error('Username is required to get simulation status')
      }
      return getSimulationStatus(username)
    },
  })
}

export const useStopSimulation = () => {
  const { username } = useSessionStore()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => {
      if (!username) {
        throw new Error('Username is required to stop simulation')
      }
      return stopSimulation(username)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['simulation'] })
    },
    onError: (error) => {
      console.error('Error stopping simulation:', error)
    },
  })
}
