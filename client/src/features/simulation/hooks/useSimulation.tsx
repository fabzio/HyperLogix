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
      simulationType?: 'simple' | 'collapse'
      originalStartDate?: string
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
    onSuccess: (_, variables) => {
      // Reset collapse state and save simulation start time
      setState({
        simulationStartTime: new Date().toISOString(),
        collapseDetected: false,
        collapseInfo: null,
        simulationType: variables.simulationType || 'simple',
        originalStartDate:
          variables.originalStartDate || variables.startTimeOrders,
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
    simulationTime, // Agregar simulationTime para usar la fecha de la simulación
  } = useSimulationStore()
  const { username } = useSessionStore()
  const queryClient = useQueryClient()

  const handleCollapse = useCallback(
    async (collapseInfo: { type: string; description: string }) => {
      try {
        // Agregar logs para depurar el problema de la fecha
        console.log('=== DEBUG COLAPSO ===')
        console.log('simulationTime actual:', simulationTime)
        console.log('Fecha del sistema:', new Date().toISOString())

        // Usar la fecha de la simulación en lugar de la fecha actual del sistema
        const collapseTimestamp = simulationTime || new Date().toISOString()
        console.log('Timestamp de colapso que se guardará:', collapseTimestamp)

        // Marcar que se detectó un colapso con timestamp
        setCollapseDetected({
          ...collapseInfo,
          timestamp: collapseTimestamp,
        })

        console.log('Información de colapso guardada:', {
          ...collapseInfo,
          timestamp: collapseTimestamp,
        })

        // Guardar métricas finales antes de detener
        if (metrics) {
          saveFinalMetrics(metrics, collapseTimestamp, plgNetwork || undefined)
        }

        if (!username) {
          console.error('Username is required to stop simulation')
          return
        }

        // Detener la simulación directamente
        await stopSimulation(username)

        console.log('Simulación detenida automáticamente debido al colapso')

        // Limpiar el estado de la simulación para activar el diálogo
        // NO borrar simulationTime aquí para que el diálogo pueda usarlo
        setState({
          plgNetwork: null,
          routes: null,
          metrics: null,
          // simulationTime: null, // ⚠️ Comentado para conservar la fecha para el diálogo
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
      simulationTime, // Agregar simulationTime a las dependencias
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

  const handleMessage = useCallback(
    (message: unknown) => {
      try {
        const typedMessage = message as MesaggeResponse
        console.log('Roadblocks received:', typedMessage.plgNetwork.roadblocks)
        setState(typedMessage)
      } catch (error) {
        console.error('Error parsing message:', error)
      }
    },
    [setState],
  )

  // Manejo de alertas de colapso
  const handleCollapseAlert = useCallback(
    (alert: unknown) => {
      try {
        const { simulationType } = useSimulationStore.getState()

        // Solo procesar alertas de colapso si estamos en modo "collapse"
        if (simulationType !== 'collapse') {
          console.log(
            'Alerta de colapso ignorada - simulación no es de tipo colapso',
          )
          return
        }

        const typedAlert = alert as {
          type: string
          collapseType?: string
          description?: string
        }
        if (typedAlert.type === 'logistic_collapse') {
          // Manejar la alerta de colapso
          console.log(
            'Colapso detectado:',
            typedAlert.collapseType,
            typedAlert.description,
          )

          // Usar el manejador de colapso que activa todo el flujo
          handleCollapse({
            type: typedAlert.collapseType || 'unknown',
            description:
              typedAlert.description || 'Colapso logístico detectado',
          })
        }
      } catch (error) {
        console.error('Error parsing collapse alert:', error)
      }
    },
    [handleCollapse],
  )

  useEffect(() => {
    if (!client || !connected) return

    // Suscripción principal a simulación
    subscribe(`/topic/simulation/${username}`, handleMessage)

    // Suscripción a alertas de colapso
    subscribe(`/topic/simulation/${username}/alerts`, handleCollapseAlert)

    return () => {
      unsubscribe(`/topic/simulation/${username}`)
      unsubscribe(`/topic/simulation/${username}/alerts`)
    }
  }, [
    subscribe,
    unsubscribe,
    connected,
    client,
    username,
    handleMessage,
    handleCollapseAlert,
  ])
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
