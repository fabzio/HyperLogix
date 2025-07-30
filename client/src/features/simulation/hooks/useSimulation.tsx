import type { PLGNetwork } from "@/domain/PLGNetwork";
import {
	commandSimulation,
	getSimulationStatus,
	startSimulation,
	stopSimulation,
} from "@/services/SimulatorService";
import { useSessionStore } from "@/store/session";
import { useWebSocketStore } from "@/store/websocket";
import {
	useMutation,
	useQueryClient,
	useSuspenseQuery,
} from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { useCallback, useEffect, useRef, useState } from "react";
import { useSimulationStore } from "../store/simulation";

type MesaggeResponse = {
	timestamp: string;
	simulationTime: string;
	plgNetwork: PLGNetwork;
};

export const useStartSimulation = () => {
	const { username } = useSessionStore();
	const { setState } = useSimulationStore();
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (params: {
			endTimeOrders: string;
			startTimeOrders: string;
			mode?: "real" | "simulation";
			simulationType?: "simple" | "collapse";
			originalStartDate?: string;
		}) => {
			if (!username) {
				throw new Error("Username is required to start simulation");
			}
			return startSimulation({
				endTimeOrders: params.endTimeOrders,
				startTimeOrders: params.startTimeOrders,
				simulationId: username,
				mode: params.mode ?? "simulation",
			});
		},
		onSuccess: (_, variables) => {
			// Reset collapse state and save simulation start time
			setState({
				simulationStartTime: new Date().toISOString(),
				collapseDetected: false,
				collapseInfo: null,
				simulationType: variables.simulationType || "simple",
				originalStartDate:
					variables.originalStartDate || variables.startTimeOrders,
				realStartSimulation: new Date(),
			});
			queryClient.invalidateQueries({ queryKey: ["simulation"] });
		},
	});
};

export const useWatchSimulation = () => {
	const { plgNetwork, simulationTime, routes, truckProgress } =
		useSimulationStore();

	return {
		plgNetwork,
		simulationTime,
		routes,
		truckProgress,
	};
};

// Hook personalizado para manejar el colapso
export const useCollapseHandler = () => {
	const {
		setCollapseDetected,
		setState,
		metrics,
		plgNetwork,
		saveFinalMetrics,
		simulationTime, // Agregar simulationTime para usar la fecha de la simulación
	} = useSimulationStore();
	const { username } = useSessionStore();
	const queryClient = useQueryClient();

	const handleCollapse = useCallback(
		async (collapseInfo: { type: string; description: string }) => {
			try {
				// Agregar logs para depurar el problema de la fecha
				console.log("=== DEBUG COLAPSO ===");
				console.log("simulationTime actual:", simulationTime);
				console.log("Fecha del sistema:", new Date().toISOString());

				// Usar la fecha de la simulación en lugar de la fecha actual del sistema
				const collapseTimestamp = simulationTime || new Date().toISOString();
				console.log("Timestamp de colapso que se guardará:", collapseTimestamp);

				// Marcar que se detectó un colapso con timestamp
				setCollapseDetected({
					...collapseInfo,
					timestamp: collapseTimestamp,
				});

				console.log("Información de colapso guardada:", {
					...collapseInfo,
					timestamp: collapseTimestamp,
				});

				// Guardar métricas finales antes de detener
				if (metrics) {
					console.log("=== GUARDANDO MÉTRICAS FINALES ===");
					console.log("Timestamp de colapso para métricas:", collapseTimestamp);
					const currentState = useSimulationStore.getState();
					console.log(
						"simulationStartTime original:",
						currentState.simulationStartTime,
					);
					console.log("originalStartDate:", currentState.originalStartDate);

					// Usar originalStartDate (que es la fecha de inicio de la simulación) como simulationStartTime
					const correctedStartTime =
						currentState.originalStartDate || currentState.simulationStartTime;
					console.log("Usando como inicio:", correctedStartTime);

					// Actualizar el simulationStartTime para que sea consistente
					setState({
						simulationStartTime: correctedStartTime,
					});

					saveFinalMetrics(metrics, collapseTimestamp, plgNetwork || undefined);
					console.log("Métricas finales guardadas correctamente");
				}

				if (!username) {
					console.error("Username is required to stop simulation");
					return;
				}

				// Detener la simulación directamente
				await stopSimulation(username);

				console.log("Simulación detenida automáticamente debido al colapso");

				// Limpiar el estado de la simulación para activar el diálogo
				// NO borrar simulationTime aquí para que el diálogo pueda usarlo
				setState({
					plgNetwork: null,
					routes: null,
					metrics: null,
					// simulationTime: null, // ⚠️ Comentado para conservar la fecha para el diálogo
				});

				// Invalidar las consultas
				queryClient.invalidateQueries({ queryKey: ["simulation"] });
			} catch (error) {
				console.error("Error al detener la simulación por colapso:", error);
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
	);

	return { handleCollapse };
};

// Nuevo hook específico para la suscripción WebSocket
export const useSimulationWebSocket = () => {
	const { setState } = useSimulationStore();
	const { username } = useSessionStore();
	const { subscribe, unsubscribe, connected, client } = useWebSocketStore();
	const { handleCollapse } = useCollapseHandler();
	// Stable message handler con logs y setState
	const handleMessageRef = useRef<((message: unknown) => void) | null>(null);
	handleMessageRef.current = (message: unknown) => {
		try {
			const typedMessage = message as MesaggeResponse;
			console.log("=== MENSAJE WEBSOCKET RECIBIDO ===");
			console.log("Mensaje completo:", typedMessage);
			console.log("Roadblocks received:", typedMessage.plgNetwork.roadblocks);
			setState(typedMessage);
		} catch (error) {
			console.error("Error parsing message:", error);
		}
	};

	// Stable collapse handler con validación de tipo
	const handleCollapseAlertRef = useRef<(alert: unknown) => void>(null);
	handleCollapseAlertRef.current = (alert: unknown) => {
		try {
			const typedAlert = alert as {
				type: string;
				collapseType?: string;
				description?: string;
			};

			console.log("Tipo de alerta:", typedAlert.type);

			if (typedAlert.type === "logistic_collapse") {
				console.log("¡ES UNA ALERTA DE COLAPSO!");
				console.log(
					"Colapso detectado:",
					typedAlert.collapseType,
					typedAlert.description,
				);
				handleCollapse({
					type: typedAlert.collapseType || "unknown",
					description: typedAlert.description || "Colapso logístico detectado",
				});
			} else {
				console.log("No es una alerta de colapso, tipo:", typedAlert.type);
			}
		} catch (error) {
			console.error("Error parsing collapse alert:", error);
		}
	};

	useEffect(() => {
		if (!client || !connected || !username) return;

		const messageHandler = (msg: unknown) => handleMessageRef.current?.(msg);
		const collapseHandler = (alert: unknown) =>
			handleCollapseAlertRef.current?.(alert);

		subscribe(`/topic/simulation/${username}`, messageHandler);
		subscribe(`/topic/simulation/${username}/alerts`, collapseHandler);

		return () => {
			unsubscribe(`/topic/simulation/${username}`);
			unsubscribe(`/topic/simulation/${username}/alerts`);
		};
	}, [client, connected, username, subscribe, unsubscribe]);
};

export const useStatusSimulation = () => {
	const { username } = useSessionStore();
	return useSuspenseQuery({
		queryKey: ["simulation"],
		queryFn: () => {
			if (!username) {
				throw new Error("Username is required to get simulation status");
			}
			return getSimulationStatus(username);
		},
		// Reducir el tiempo de refetch para actualizaciones más rápidas
		refetchInterval: 500, // 500ms en lugar del default
		refetchIntervalInBackground: true,
		// Asegurar que siempre obtenga datos frescos
		staleTime: 0,
		gcTime: 1000 * 10, // 10 segundos de cache
	});
};

export const useStopSimulation = () => {
	const navigate = useNavigate({ from: "/simulacion" });
	const {
		setState,
		metrics,
		plgNetwork,
		saveFinalMetrics,
		collapseDetected,
		simulationTime,
	} = useSimulationStore();
	const { username } = useSessionStore();
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: () => {
			if (!username) {
				throw new Error("Username is required to stop simulation");
			}
			return stopSimulation(username);
		},
		onSuccess: () => {
			// Solo guardar métricas si NO fue por colapso (para evitar sobrescribir)
			if (metrics && !collapseDetected) {
				console.log("=== GUARDANDO MÉTRICAS STOP MANUAL ===");
				const currentState = useSimulationStore.getState();
				console.log("simulationTime actual:", simulationTime);
				console.log("originalStartDate:", currentState.originalStartDate);

				// Para stop manual: usar originalStartDate como inicio y simulationTime como fin
				const correctedStartTime =
					currentState.originalStartDate || currentState.simulationStartTime;
				const stopTimestamp = simulationTime || new Date().toISOString();

				console.log("Usando como inicio (stop manual):", correctedStartTime);
				console.log("Usando como fin (stop manual):", stopTimestamp);

				// Actualizar el simulationStartTime para que sea consistente con la fecha de inicio de simulación
				setState({
					simulationStartTime: correctedStartTime,
				});

				saveFinalMetrics(
					metrics,
					stopTimestamp, // Usar el tiempo actual de la simulación como fin
					plgNetwork || undefined,
				);
				console.log("Métricas stop manual guardadas correctamente");
			} else if (collapseDetected) {
				console.log("=== NO GUARDANDO MÉTRICAS - YA GUARDADAS POR COLAPSO ===");
			}

			setState({
				plgNetwork: null,
				simulationTime: null,
				routes: null,
				metrics: null,
				realStartSimulation: null,
			});
			navigate({
				to: "/simulacion",
				search: { truckId: undefined, orderId: undefined },
			});
			queryClient.invalidateQueries({ queryKey: ["simulation"] });
		},
		onError: (error) => {
			console.error("Error stopping simulation:", error);
		},
	});
};
export const useSimulationEndDialog = (network: PLGNetwork | null) => {
	const [isOpen, setIsOpen] = useState(false);
	const [endReason, setEndReason] = useState<
		"completed" | "manual" | "collapse" | null
	>(null);
	const {
		metrics,
		plgNetwork,
		saveFinalMetrics,
		collapseDetected,
		simulationType,
		setCollapseDetected,
		simulationTime,
	} = useSimulationStore();
	const { mutate: stopSimulation } = useStopSimulation();
	const { username } = useSessionStore();

	const wasActiveRef = useRef(false);
	const prevAllCompletedRef = useRef(false);
	const prevCollapseDetectedRef = useRef(false);

	useEffect(() => {
		const isActive = !!network;

		console.log("=== DEBUG useSimulationEndDialog ===");
		console.log("collapseDetected:", collapseDetected);
		console.log("simulationType:", simulationType);
		console.log(
			"Condición se cumple:",
			collapseDetected &&
				!prevCollapseDetectedRef.current &&
				simulationType === "collapse",
		);

		// Check for collapse detection change
		if (
			collapseDetected &&
			!prevCollapseDetectedRef.current &&
			simulationType === "collapse"
		) {
			console.log("=== DETECTADO COLAPSO EN DIALOG ===");
			console.log("metrics disponible:", !!metrics);
			console.log("collapseDetected tipo:", typeof collapseDetected);
			console.log("collapseDetected.timestamp:", collapseDetected?.timestamp);

			// Guardar métricas finales con el timestamp correcto del colapso
			if (
				metrics &&
				collapseDetected &&
				typeof collapseDetected === "object" &&
				collapseDetected.timestamp
			) {
				console.log("=== GUARDANDO MÉTRICAS DESDE DIALOG COLAPSO ===");
				console.log("Timestamp del colapso:", collapseDetected.timestamp);
				console.log(
					"simulationStartTime:",
					useSimulationStore.getState().simulationStartTime,
				);
				saveFinalMetrics(
					metrics,
					collapseDetected.timestamp,
					network || plgNetwork || undefined,
				);
				console.log("Métricas finales guardadas desde dialog");
			} else {
				console.log("=== NO SE PUEDEN GUARDAR MÉTRICAS ===");
				console.log(
					"Razón: metrics =",
					!!metrics,
					", collapseDetected =",
					!!collapseDetected,
					", timestamp =",
					collapseDetected?.timestamp,
				);
			}

			setEndReason("collapse");
			setIsOpen(true);
			prevCollapseDetectedRef.current = collapseDetected;
			return;
		}

		// Check for collapse in simple simulation - treat as manual stop
		if (
			collapseDetected &&
			!prevCollapseDetectedRef.current &&
			simulationType === "simple"
		) {
			setEndReason("manual");
			setIsOpen(true);
			prevCollapseDetectedRef.current = collapseDetected;
			return;
		}

		// Check for manual stop (only if not a collapse)
		if (!collapseDetected && wasActiveRef.current && !isActive) {
			setEndReason("manual");
			setIsOpen(true);
		}

		// Check for completion
		if (network?.orders?.length) {
			const allCompleted = network.orders.every(
				(order) => order.status === "COMPLETED",
			);
			if (allCompleted && !prevAllCompletedRef.current) {
				// Save final metrics when simulation completes
				if (metrics) {
					saveFinalMetrics(
						metrics,
						new Date().toISOString(),
						network || plgNetwork || undefined,
					);
				}

				// Detener la simulación automáticamente cuando se complete
				if (username) {
					console.log(
						"Todos los pedidos completados - deteniendo simulación automáticamente",
					);
					stopSimulation();
				}
				if (simulationType === "collapse")
					setCollapseDetected({
						timestamp: simulationTime || new Date().toISOString(),
						type: "colapse",
						description:
							"No hay camiones disponibles para completar las entregas.",
					});
				setEndReason(simulationType === "simple" ? "completed" : "collapse");
				setIsOpen(true);
			}
			prevAllCompletedRef.current = allCompleted;
		}

		wasActiveRef.current = isActive;
		prevCollapseDetectedRef.current = collapseDetected;
	}, [
		network,
		metrics,
		saveFinalMetrics,
		plgNetwork,
		collapseDetected,
		simulationType,
		stopSimulation,
		username,
	]);

	const closeDialog = () => {
		setIsOpen(false);
		// Reset collapse state when closing dialog
		const { setState } = useSimulationStore.getState();
		setState({ collapseDetected: false, collapseInfo: null });
	};

	return { isOpen, endReason, closeDialog };
};

export const useCommandSimulation = () => {
	const { username } = useSessionStore();
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (params: {
			command: "PAUSE" | "RESUME" | "DESACCELERATE" | "ACCELERATE";
		}) => {
			if (!username) {
				throw new Error("Username is required to command simulation");
			}
			return commandSimulation(username, params.command);
		},
		onSuccess: () =>
			queryClient.invalidateQueries({ queryKey: ["simulation"] }),
	});
};
