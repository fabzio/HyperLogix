import DynamicMap, { type MapPolyline } from '@/components/DynamicMap'
import { Button } from '@/components/ui/button'
import { useStartBenchmark, useWatchBenchmark } from './hooks/useBenchmark'

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
  const { routes } = useWatchBenchmark()
  const { data: network, refetch } = useStartBenchmark()
  const poliLines: MapPolyline[] = []
  const pointMarkers: [number, number][] = []
  if (routes) {
    const {
      cost: _,
      routes: { paths, stops },
    } = routes
    for (const [key, path] of Object.entries(paths)) {
      const points = path.flatMap((p) => p.points)
      poliLines.push({
        type: 'path',
        points: points
          .map((p) => (p ? [p.x, p.y] : null))
          .filter((p): p is [number, number] => p !== null),
        stroke: getColorFromKey(key), // Assign a color based on the key
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
  console.log('Network', network)

  return (
    <section className="flex flex-col gap-4">
      <div className="flex justify-center items-center">
        <Button
          onClick={() => {
            refetch()
          }}
        >
          Empezar
        </Button>
      </div>
      <DynamicMap
        trucks={network?.trucks || []}
        stations={network?.stations || []}
        orders={network?.orders || []}
        polylines={poliLines}
        points={pointMarkers}
      />
    </section>
  )
}
