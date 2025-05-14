import type { MapPolyline } from '@/components/DynamicMap/index'
import { Button } from '@/components/ui/button'
import { useState } from 'react'
import { useStartBenchmark, useWatchBenchmark } from './hooks/useBenchmark'
import DynamicMap from '@/components/DynamicMap/index'

// Function to generate a color based on a string key
const getColorFromKey = (key: string): string => {
  const hash = Array.from(key).reduce(
    (acc, char) => acc + char.charCodeAt(0),
    0,
  )
  const hue = hash % 360 // Generate a hue value between 0 and 359
  return `hsl(${hue}, 80%, 40%)` // Adjust saturation to 80% and lightness to 40% for better visibility
}

export default function Benchmark() {
  const { data: network, refetch } = useStartBenchmark()
  const { routes } = useWatchBenchmark()
  const [hoveredTruckId, setHoveredTruckId] = useState<string | null>(null)

  const poliLines: MapPolyline[] = []
  const pointMarkers: [number, number][] = []

  const handlePolylineHover = (truckId: string | null) => {
    setHoveredTruckId(truckId)
  }

  const handlePolylineClick = (truckId: string, polyline: MapPolyline) => {
    console.log('Clicked Truck ID:', truckId)
    console.log('Clicked Polyline Path:', polyline.points)
    if (routes?.routes?.stops?.[truckId]) {
      console.log('Associated Stops:', routes.routes.stops[truckId])
    }
  }

  if (routes) {
    const {
      cost: _,
      routes: { paths, stops },
    } = routes
    for (const [key, path] of Object.entries(paths)) {
      if (hoveredTruckId && hoveredTruckId !== key) {
        continue
      }
      const points = path.flatMap((p) => p.points)
      poliLines.push({
        type: 'path',
        points: points
          .map((p) => (p ? [p.x, p.y] : null))
          .filter((p): p is [number, number] => p !== null),
        stroke: getColorFromKey(key),
        strokeWidth: 0.5,
        id: key,
      })
      const stop = stops[key]
      pointMarkers.push(
        ...stop.map((s) => {
          return [s.node.location.x, s.node.location.y] as [number, number]
        }),
      )
    }
  }

  return (
    <section className="flex flex-col">
      <DynamicMap
        trucks={network?.trucks || []}
        stations={network?.stations || []}
        orders={network?.orders || []}
        polylines={poliLines}
        onPolylineHover={handlePolylineHover}
        onPolylineClick={handlePolylineClick}
        hoveredPolylineId={hoveredTruckId}
      />
    </section>
  )
}
