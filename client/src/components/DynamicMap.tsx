import { geoIdentity } from 'd3-geo'
import { useState } from 'react'
import {
  ComposableMap,
  Line,
  Marker,
  type ProjectionFunction,
  ZoomableGroup,
} from 'react-simple-maps'

export default function DynamicMap() {
  const [zoom, setZoom] = useState(1)
  const [center, setCenter] = useState<[number, number]>([0, 0])
  const CELL_SIZE = 5
  const GRID_WIDTH = 70
  const GRID_HEIGHT = 50

  const verticalLines = []
  const horizontalLines = []

  for (let y = 0; y < GRID_WIDTH; y++) {
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
  for (let x = 0; x < GRID_HEIGHT; x++) {
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

  return (
    <div
      style={{
        padding: '1rem',
        display: 'flex',
        justifyContent: 'center',
        overflow: 'visible',
      }}
    >
      <div style={{ width: '90vw', height: 'auto' }}>
        <ComposableMap
          projection={geoIdentity() as unknown as ProjectionFunction}
          projectionConfig={{ scale: 1 }}
          viewBox={`0 0 ${GRID_WIDTH * CELL_SIZE} ${GRID_HEIGHT * CELL_SIZE}`}
          style={{ width: '100%', height: 'auto', cursor: 'grab' }}
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
            <Marker coordinates={[0, 0]}>
              <circle r={2} fill="#FF5533" stroke="#FFF" strokeWidth={0.1} />
            </Marker>
          </ZoomableGroup>
        </ComposableMap>
      </div>
    </div>
  )
}
