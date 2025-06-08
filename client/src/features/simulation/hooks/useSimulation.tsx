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
import { useCallback, useEffect, useRef, useState } from 'react'
import { useSimulationStore } from '../store/simulation'

type MesaggeResponse = {
  timestamp: string
  simulationTime: string
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
  const { plgNetwork, simulationTime, setState, routes } = useSimulationStore()
  const { username } = useSessionStore()
  const { subscribe, unsubscribe, connected, client } = useWebSocketStore()

  const handleMessage = useCallback(
    (message: unknown) => {
      try {
        const typedMessage = message as MesaggeResponse
        //console.log(typedMessage.plgNetwork.roadblocks)
        setState(typedMessage)
      } catch (error) {
        console.error('Error parsing message:', error)
      }
    },
    [setState],
  )

  useEffect(() => {
    if (!client || !connected) return
    subscribe(`/topic/simulation/${username}`, handleMessage)
    return () => {
      unsubscribe(`/topic/simulation/${username}`)
    }
  }, [subscribe, unsubscribe, connected, client, username, handleMessage])

  return {
    plgNetwork,
    simulationTime,
    routes,
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
  const { setState } = useSimulationStore()
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
      setState({
        plgNetwork: null,
        simulationTime: null,
        routes: null,
        metrics: null,
      })
      queryClient.invalidateQueries({ queryKey: ['simulation'] })
    },
    onError: (error) => {
      console.error('Error stopping simulation:', error)
    },
  })
}
export const useSimulationEndDialog = (network: PLGNetwork | null) => {
  const [isOpen, setIsOpen] = useState(false)
  const [endReason, setEndReason] = useState<'completed' | 'manual' | null>(
    null,
  )

  const wasActiveRef = useRef(false)
  const prevAllCompletedRef = useRef(false)

  useEffect(() => {
    const isActive = !!network

    // Check for manual stop
    if (wasActiveRef.current && !isActive) {
      setEndReason('manual')
      setIsOpen(true)
    }

    // Check for completion
    if (network?.orders?.length) {
      const allCompleted = network.orders.every(
        (order) => order.status === 'COMPLETED',
      )
      if (allCompleted && !prevAllCompletedRef.current) {
        setEndReason('completed')
        setIsOpen(true)
      }
      prevAllCompletedRef.current = allCompleted
    }

    wasActiveRef.current = isActive
  }, [network])

  const closeDialog = () => setIsOpen(false)

  return { isOpen, endReason, closeDialog }
}
