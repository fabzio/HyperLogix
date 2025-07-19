import type { PLGNetwork } from '@/domain/PLGNetwork'
import {
  commandSimulation,
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
import { useNavigate } from '@tanstack/react-router'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useSimulationStore } from '../store/simulation'

type MesaggeResponse = {
  timestamp: string
  simulationTime: string
  plgNetwork: PLGNetwork
}

export const useStartSimulation = () => {
  const { username } = useSessionStore()
  const { setState } = useSimulationStore()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: {
      endTimeOrders: string
      startTimeOrders: string
      mode?: 'real' | 'simulation'
    }) => {
      if (!username) {
        throw new Error('Username is required to start simulation')
      }
      return startSimulation({
        endTimeOrders: params.endTimeOrders,
        startTimeOrders: params.startTimeOrders,
        simulationId: username,
        mode: params.mode ?? 'simulation',
      })
    },
    onSuccess: () => {
      // Save simulation start time
      setState({ simulationStartTime: new Date().toISOString() })
      queryClient.invalidateQueries({ queryKey: ['simulation'] })
    },
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
  const navigate = useNavigate({ from: '/simulacion' })
  const { setState, metrics, plgNetwork, saveFinalMetrics } =
    useSimulationStore()
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
      // Save final metrics before clearing state
      if (metrics) {
        saveFinalMetrics(
          metrics,
          new Date().toISOString(),
          plgNetwork || undefined,
        )
      }

      setState({
        plgNetwork: null,
        simulationTime: null,
        routes: null,
        metrics: null,
      })
      navigate({
        to: '/simulacion',
        search: { truckId: undefined, orderId: undefined },
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
  const { metrics, plgNetwork, saveFinalMetrics } = useSimulationStore()

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
        // Save final metrics when simulation completes
        if (metrics) {
          saveFinalMetrics(
            metrics,
            new Date().toISOString(),
            network || plgNetwork || undefined,
          )
        }
        setEndReason('completed')
        setIsOpen(true)
      }
      prevAllCompletedRef.current = allCompleted
    }

    wasActiveRef.current = isActive
  }, [network, metrics, saveFinalMetrics, plgNetwork])

  const closeDialog = () => setIsOpen(false)

  return { isOpen, endReason, closeDialog }
}

export const useCommandSimulation = () => {
  const { username } = useSessionStore()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: {
      command: 'PAUSE' | 'RESUME' | 'DESACCELERATE' | 'ACCELERATE'
    }) => {
      if (!username) {
        throw new Error('Username is required to start simulation')
      }
      return commandSimulation(username, params.command)
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['simulation'] }),
  })
}
