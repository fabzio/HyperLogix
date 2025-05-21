import { Line, Marker } from 'react-simple-maps'
import type { MapPolyline } from './index'

const HOVER_STROKE_WIDTH = 1.5
const HIT_AREA_STROKE_WIDTH = 3

interface PolylineItemProps {
  polyline: MapPolyline
  transformRawPoint: (point: [number, number]) => [number, number]
  isHovered: boolean
  onPolylineHover?: (truckId: string | null) => void
  onPolylineClick?: (truckId: string, polyline: MapPolyline) => void
  hoveredPolylineId?: string | null
}

const PolylineItem = ({
  polyline,
  transformRawPoint,
  isHovered,
  onPolylineHover,
  onPolylineClick,
}: PolylineItemProps) => {
  if (polyline.points.length < 2) return null

  const transformedPolylinePoints = polyline.points.map(transformRawPoint)

  const lineSegments = transformedPolylinePoints
    .slice(0, -1)
    .map((p1, index) => {
      const p2 = transformedPolylinePoints[index + 1]
      let strokeColor = polyline.stroke || '#777777'
      let strokeDashArray: string | undefined = undefined
      let strokeW = polyline.strokeWidth || 0.3

      if (isHovered) {
        strokeW = HOVER_STROKE_WIDTH
      }

      if (polyline.type === 'path') {
        strokeDashArray = '2 2'
      } else if (polyline.type === 'roadblock') {
        strokeColor = 'red'
      }

      return (
        <g key={`${polyline.id}-segment-group-${index}`}>
          <Line
            key={`${polyline.id}-segment-${index}-hitbox`}
            coordinates={[p1, p2]}
            stroke="transparent"
            strokeWidth={HIT_AREA_STROKE_WIDTH}
          />
          <Line
            key={`${polyline.id}-segment-${index}-visible`}
            coordinates={[p1, p2]}
            stroke={strokeColor}
            strokeWidth={strokeW}
            strokeDasharray={strokeDashArray}
          />
        </g>
      )
    })

  let startPointMarker = null
  let endPointMarker = null
  if (
    (polyline.type === 'path' || polyline.type === 'roadblock') &&
    transformedPolylinePoints.length > 0
  ) {
    startPointMarker = (
      <Marker
        key={`${polyline.id}-start-marker`}
        coordinates={transformedPolylinePoints[0]}
      >
        <circle
          r={1.5}
          fill={
            polyline.type === 'roadblock' ? 'red' : polyline.stroke || '#3355FF'
          }
          stroke={polyline.type === 'roadblock' ? 'black' : '#FFF'}
          strokeWidth={0.2}
        />
      </Marker>
    )
    endPointMarker = (
      <Marker
        key={`${polyline.id}-end-marker`}
        coordinates={
          transformedPolylinePoints[transformedPolylinePoints.length - 1]
        }
      >
        <circle
          r={1.5}
          fill={
            polyline.type === 'roadblock' ? 'red' : polyline.stroke || '#3355FF'
          }
          stroke={polyline.type === 'roadblock' ? 'black' : '#FFF'}
          strokeWidth={0.2}
        />
      </Marker>
    )
  }

  const handleMouseEnter = () => {
    onPolylineHover?.(polyline.id)
  }
  const handleMouseLeave = () => {
    onPolylineHover?.(null)
  }
  const handleClick = () => {
    onPolylineClick?.(polyline.id, polyline)
  }
  const handleKeyDown = (e: React.KeyboardEvent<SVGElement>) => {
    if (e.key === 'Enter' || e.key === ' ') {
      onPolylineClick?.(polyline.id, polyline)
    }
  }

  return (
    <g
      key={`${polyline.id}-group`}
      data-polyline
      data-selected={isHovered ? 'true' : undefined}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      style={{ cursor: 'pointer' }}
      tabIndex={0}
    >
      {lineSegments}
      {startPointMarker}
      {endPointMarker}
    </g>
  )
}

export default PolylineItem
