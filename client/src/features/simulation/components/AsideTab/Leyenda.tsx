import {
  Truck,
  AlertCircle,
  Slash,
  Fuel,
  XCircle,
  Building2,
} from 'lucide-react'

/*function getTruckColor(truckId: string): string {
    const hue = (truckId.charCodeAt(0) * 137.5) % 360
    return `hsl(${hue}, 70%, 50%)`
}*/

export default function Leyenda() {
  return (
    <div className="border rounded-md bg-muted/50 p-4 text-sm w-fit shadow-md space-y-4">
      <h3 className="text-blue-600 font-semibold text-base">Leyenda</h3>

      <div className="grid grid-cols-2 gap-x-4 gap-y-2">
        <LegendItem
          icon={<Truck className="text-muted-foreground" />}
          label="Vehículo activo"
        />
        <LegendItem
          icon={<Building2 className="text-muted-foreground" />}
          label="Planta principal"
        />
        <LegendItem icon={<DestinoIcon />} label="Punto de destino" />
        <LegendItem
          icon={<Fuel className="text-muted-foreground" />}
          label="Tanque secundario"
        />
        <LegendItem
          icon={
            <div className="relative">
              <div className="w-4 h-4 rounded-full bg-red-500 border border-black" />
              <div className="absolute inset-1 rounded-full bg-black" />
            </div>
          }
          label="Nodo de bloqueo"
        />
        <LegendItem
          icon={<Slash className="text-red-700 rotate-45" />}
          label="Segmento bloqueado"
        />
        <LegendItem
          icon={
            <div className="w-6 h-0.5 border-t-2 border-dashed border-white" />
          }
          label="Trayecto o ruta del camion"
        />
        <LegendItem
          icon={<XCircle className="text-muted-foreground" />}
          label="Fuera de servicio"
        />
      </div>

      <hr className="border-muted" />

      <div className="grid grid-cols-2 gap-2 text-xs">
        <ColorLabel color="bg-purple-500" label="Camiones tipo TA" />
        <ColorLabel color="bg-green-400" label="Camiones tipo TC" />
        <ColorLabel color="bg-sky-400" label="Camiones tipo TB" />
        <ColorLabel color="bg-yellow-400" label="Camiones tipo TD" />
      </div>
    </div>
  )
}

function LegendItem({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <div className="flex items-center gap-2">
      <div className="w-5 h-5 flex items-center justify-center">{icon}</div>
      <span>{label}</span>
    </div>
  )
}

function ColorLabel({ color, label }: { color: string; label: string }) {
  return (
    <div className="flex items-center gap-2">
      <div className={`w-3 h-3 rounded-full ${color}`} />
      <span>{label}</span>
    </div>
  )
}

// Ícono para el destino
function DestinoIcon() {
  return (
    <div className="relative w-5 h-5 bg-white border border-gray-400 rounded-sm">
      <div className="absolute top-[2px] left-1/2 -translate-x-1/2 w-1.5 h-1.5 bg-black rounded-full" />
      <div className="absolute bottom-[2px] left-[2px] right-[2px] h-0.5 bg-black" />
    </div>
  )
}
export { default as Leyenda } from './Leyenda'
