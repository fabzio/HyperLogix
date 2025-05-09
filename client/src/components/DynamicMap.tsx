import OrderIcon from '@/components/icons/OrderIcon'
import StationIcon from '@/components/icons/StationIcon'
import TruckIcon from '@/components/icons/TruckIcon'
import type { Location } from '@/domain/Location'
import type { Order } from '@/domain/Order'
import type { Station } from '@/domain/Station'
import type { Truck } from '@/domain/Truck'
import { geoIdentity } from 'd3-geo'
import { useState } from 'react'
import {
  ComposableMap,
  Line,
  Marker,
  type ProjectionFunction,
  ZoomableGroup,
} from 'react-simple-maps'

export interface MapPolyline {
  id: string
  points: [number, number][]
  stroke?: string
  strokeWidth?: number
  type?: 'path' | 'roadblock'
}

interface DynamicMapProps {
  points?: [number, number][]
  trucks?: Truck[]
  stations?: Station[]
  orders?: Order[]
  polylines?: MapPolyline[]
}

const CELL_SIZE = 5
const GRID_WIDTH = 70
const GRID_HEIGHT = 50

export default function DynamicMap({
  points = [],
  trucks = [],
  stations = [],
  orders = [],
  polylines = [],
}: DynamicMapProps) {
  const [zoom, setZoom] = useState(1)
  const [center, setCenter] = useState<[number, number]>([0, 0])

  const customProjection = geoIdentity() as unknown as ProjectionFunction

  const verticalLines = []
  const horizontalLines = []

  for (let y = 0; y <= GRID_WIDTH; y++) {
    verticalLines.push(
      <Line
        key={`v-${y}`}
        coordinates={[
          [y * CELL_SIZE, 0],
          [y * CELL_SIZE, GRID_HEIGHT * CELL_SIZE],
        ]}
        stroke="#000"
        strokeWidth={0.1}
      />,
    )
  }

  for (let x = 0; x <= GRID_HEIGHT; x++) {
    horizontalLines.push(
      <Line
        key={`h-${x}`}
        coordinates={[
          [0, x * CELL_SIZE],
          [GRID_WIDTH * CELL_SIZE, x * CELL_SIZE],
        ]}
        stroke="#000"
        strokeWidth={0.1}
      />,
    )
  }

  const transformedPoints = points.map(([x, y]) => [
    x * CELL_SIZE,
    (GRID_HEIGHT - y) * CELL_SIZE,
  ])

  const transformLocation = (location: Location) => [
    location.x * CELL_SIZE,
    (GRID_HEIGHT - location.y) * CELL_SIZE,
  ]

  const transformRawPoint = (point: [number, number]): [number, number] => [
    point[0] * CELL_SIZE,
    (GRID_HEIGHT - point[1]) * CELL_SIZE,
  ]

  return (
    <div className="h-screen p-4 flex flex-col justify-center overflow-hidden box-border">
      <div className="w-full h-full max-h-[calc(100vh-2rem)] flex justify-center items-center">
        <div
          className={`w-full h-full aspect-[${GRID_WIDTH}/${GRID_HEIGHT}]`}
          style={{
            maxWidth: `calc((100vh - 2rem) * ${GRID_WIDTH / GRID_HEIGHT})`,
            maxHeight: `calc((100vw) * ${GRID_HEIGHT / GRID_WIDTH})`,
          }}
        >
          <ComposableMap
            projection={customProjection}
            projectionConfig={{ scale: 1 }}
            viewBox={`0 0 ${GRID_WIDTH * CELL_SIZE} ${GRID_HEIGHT * CELL_SIZE}`}
            className="w-full h-full cursor-grab"
          >
            <ZoomableGroup
              zoom={zoom}
              center={center}
              onMoveEnd={({ zoom, coordinates }) => {
                setZoom(zoom)
                setCenter(coordinates)
              }}
              translateExtent={[
                [0, 0],
                [GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE],
              ]}
            >
              {verticalLines}
              {horizontalLines}

              {/* Origin marker */}
              <Marker coordinates={[0, GRID_HEIGHT * CELL_SIZE]}>
                <circle r={2} fill="#FF5533" stroke="#FFF" strokeWidth={0.1} />
              </Marker>

              {/* User-provided points */}
              {transformedPoints.map(([x, y]) => (
                <Marker key={`point-${x}-${y}`} coordinates={[x, y]}>
                  <circle
                    r={1.5}
                    fill="#3355FF"
                    stroke="#FFF"
                    strokeWidth={0.1}
                  />
                </Marker>
              ))}

              {/* Render Trucks */}
              {trucks.map((truck) => {
                const [cx, cy] = transformLocation(truck.location)
                return (
                  <TruckMarker key={truck.id} truck={truck} cx={cx} cy={cy} />
                )
              })}

              {/* Render Stations */}
              {stations.map((station) => {
                const [cx, cy] = transformLocation(station.location)
                return (
                  <StationMarker
                    key={station.id}
                    station={station}
                    cx={cx}
                    cy={cy}
                  />
                )
              })}

              {/* Render Orders */}
              {orders.map((order) => {
                const [cx, cy] = transformLocation(order.location)
                return (
                  <OrderMarker key={order.id} order={order} cx={cx} cy={cy} />
                )
              })}

              {/* Render Polylines */}
              {polylines.map((polyline) => {
                if (polyline.points.length < 2) return null

                const transformedPolylinePoints =
                  polyline.points.map(transformRawPoint)

                const lineSegments = transformedPolylinePoints
                  .slice(0, -1)
                  .map((p1, index) => {
                    const p2 = transformedPolylinePoints[index + 1]
                    let strokeColor = polyline.stroke || '#777777'
                    let strokeDashArray: string | undefined = undefined
                    const strokeW = polyline.strokeWidth || 0.3

                    if (polyline.type === 'path') {
                      strokeDashArray = '2 2' // Dashed line for paths
                      // strokeColor is already polyline.stroke || '#777777'
                    } else if (polyline.type === 'roadblock') {
                      strokeColor = 'red' // Red line for roadblocks
                    }

                    return (
                      <Line
                        key={`${polyline.id}-segment-${index}`}
                        coordinates={[p1, p2]}
                        stroke={strokeColor}
                        strokeWidth={strokeW}
                        strokeDasharray={strokeDashArray}
                      />
                    )
                  })

                const startPointMarker =
                  polyline.type === 'path' || polyline.type === 'roadblock' ? (
                    <Marker
                      key={`${polyline.id}-start-marker`}
                      coordinates={transformedPolylinePoints[0]}
                    >
                      <circle
                        r={1.5}
                        fill={
                          polyline.type === 'roadblock'
                            ? 'red'
                            : polyline.stroke || '#3355FF' // Use polyline stroke color for path, else default
                        }
                        stroke={
                          polyline.type === 'roadblock' ? 'black' : '#FFF'
                        }
                        strokeWidth={0.2}
                      />
                    </Marker>
                  ) : null

                const endPointMarker =
                  polyline.type === 'path' || polyline.type === 'roadblock' ? (
                    <Marker
                      key={`${polyline.id}-end-marker`}
                      coordinates={
                        transformedPolylinePoints[
                        transformedPolylinePoints.length - 1
                        ]
                      }
                    >
                      <circle
                        r={1.5}
                        fill={
                          polyline.type === 'roadblock'
                            ? 'red'
                            : polyline.stroke || '#3355FF' // Use polyline stroke color for path, else default
                        }
                        stroke={
                          polyline.type === 'roadblock' ? 'black' : '#FFF'
                        }
                        strokeWidth={0.2}
                      />
                    </Marker>
                  ) : null

                return (
                  <g key={`${polyline.id}-group`}>
                    {lineSegments}
                    {startPointMarker}
                    {endPointMarker}
                  </g>
                )
              })}
            </ZoomableGroup>
          </ComposableMap>
        </div>
      </div>
    </div>
  )
}

const formatTooltipText = (obj: object) => {
  return Object.entries(obj)
    .map(([key, value]) => `${key}: ${value}`)
    .join('\n')
}

const TruckMarker = ({
  truck,
  cx,
  cy,
}: {
  truck: Truck
  cx: number
  cy: number
}) => (
  <Marker coordinates={[cx - 2, cy + 2]} onClick={() => console.log(truck)}>
    <TruckIcon />
    <title>{formatTooltipText(truck)}</title>
  </Marker>
)

const StationMarker = ({
  station,
  cx,
  cy,
}: {
  station: Station
  cx: number
  cy: number
}) => (
  <Marker coordinates={[cx - 2, cy + 2]}>
    <StationIcon />
    <title>{formatTooltipText(station)}</title>
  </Marker>
)

const OrderMarker = ({
  order,
  cx,
  cy,
}: {
  order: Order
  cx: number
  cy: number
}) => (
  <Marker coordinates={[cx - 2, cy + 2]}>
    <OrderIcon />
    <title>{formatTooltipText(order)}</title>
  </Marker>
)
