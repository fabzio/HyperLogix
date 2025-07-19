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
      // Reset collapse state and save simulation start time
      setState({
        simulationStartTime: new Date().toISOString(),
        collapseDetected: false,
        collapseInfo: null,
      })
      queryClient.invalidateQueries({ queryKey: ['simulation'] })
    },
  })
}

export const useWatchSimulation = () => {
  const { plgNetwork, simulationTime, routes } = useSimulationStore()

  return {
    plgNetwork,
    simulationTime,
    routes,
  }
}

// Hook personalizado para manejar el colapso
export const useCollapseHandler = () => {
  const {
    setCollapseDetected,
    setState,
    metrics,
    plgNetwork,
    saveFinalMetrics,
  } = useSimulationStore()
  const { username } = useSessionStore()
  const queryClient = useQueryClient()

  const handleCollapse = useCallback(
    async (collapseInfo: { type: string; description: string }) => {
      try {
        // Marcar que se detectó un colapso
        setCollapseDetected(collapseInfo)

        // Guardar métricas finales antes de detener
        if (metrics) {
          saveFinalMetrics(
            metrics,
            new Date().toISOString(),
            plgNetwork || undefined,
          )
        }

        if (!username) {
          console.error('Username is required to stop simulation')
          return
        }

        // Detener la simulación directamente
        await stopSimulation(username)

        console.log('Simulación detenida automáticamente debido al colapso')

        // Limpiar el estado de la simulación para activar el diálogo
        setState({
          plgNetwork: null,
          simulationTime: null,
          routes: null,
          metrics: null,
        })

        // Invalidar las consultas
        queryClient.invalidateQueries({ queryKey: ['simulation'] })
      } catch (error) {
        console.error('Error al detener la simulación por colapso:', error)
      }
    },
    [
      setCollapseDetected,
      setState,
      metrics,
      plgNetwork,
      saveFinalMetrics,
      username,
      queryClient,
    ],
  )

  return { handleCollapse }
}

// Nuevo hook específico para la suscripción WebSocket
export const useSimulationWebSocket = () => {
  const { setState } = useSimulationStore()
  const { username } = useSessionStore()
  const { subscribe, unsubscribe, connected, client } = useWebSocketStore()
  const { handleCollapse } = useCollapseHandler()

  // Stable message handler
  const handleMessageRef = useRef<(message: unknown) => void>()
  handleMessageRef.current = (message: unknown) => {
    try {
      const typedMessage = message as MesaggeResponse
      setState(typedMessage)
    } catch (error) {
      console.error('Error parsing message:', error)
    }
  }

  // Stable collapse handler
  const handleCollapseAlertRef = useRef<(alert: unknown) => void>()
  handleCollapseAlertRef.current = (alert: unknown) => {
    try {
      const typedAlert = alert as {
        type: string
        collapseType?: string
        description?: string
      }
      if (typedAlert.type === 'logistic_collapse') {
        handleCollapse({
          type: typedAlert.collapseType || 'unknown',
          description: typedAlert.description || 'Colapso logístico detectado',
        })
      }
    } catch (error) {
      console.error('Error parsing collapse alert:', error)
    }
  }

  useEffect(() => {
    if (!client || !connected || !username) return

    // Use stable refs for handlers
    const messageHandler = (msg: unknown) => handleMessageRef.current?.(msg)
    const collapseHandler = (alert: unknown) =>
      handleCollapseAlertRef.current?.(alert)

    subscribe(`/topic/simulation/${username}`, messageHandler)
    subscribe(`/topic/simulation/${username}/alerts`, collapseHandler)

    return () => {
      unsubscribe(`/topic/simulation/${username}`)
      unsubscribe(`/topic/simulation/${username}/alerts`)
    }
  }, [client, connected, username, subscribe, unsubscribe])
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
  const [endReason, setEndReason] = useState<
    'completed' | 'manual' | 'collapse' | null
  >(null)
  const { metrics, plgNetwork, saveFinalMetrics, collapseDetected } =
    useSimulationStore()

  const wasActiveRef = useRef(false)
  const prevAllCompletedRef = useRef(false)
  const prevCollapseDetectedRef = useRef(false)

  useEffect(() => {
    const isActive = !!network

    // Check for collapse detection change
    if (collapseDetected && !prevCollapseDetectedRef.current) {
      setEndReason('collapse')
      setIsOpen(true)
      prevCollapseDetectedRef.current = collapseDetected
      return
    }

    // Check for manual stop (only if not a collapse)
    if (!collapseDetected && wasActiveRef.current && !isActive) {
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
    prevCollapseDetectedRef.current = collapseDetected
  }, [network, metrics, saveFinalMetrics, plgNetwork, collapseDetected])

  const closeDialog = () => {
    setIsOpen(false)
    // Reset collapse state when closing dialog
    const { setState } = useSimulationStore.getState()
    setState({ collapseDetected: false, collapseInfo: null })
  }

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
        throw new Error('Username is required to command simulation')
      }
      return commandSimulation(username, params.command)
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['simulation'] }),
  })
}
