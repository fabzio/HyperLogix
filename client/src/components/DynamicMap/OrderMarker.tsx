import type { Order } from '@/domain/Order'
import { Receipt } from 'lucide-react'
import { memo } from 'react'
import { Marker } from 'react-simple-maps'
import { formatTooltipText } from './index'

const OrderMarker = memo(
  ({
    order,
    cx,
    cy,
    onClick,
  }: {
    order: Order
    cx: number
    cy: number
    onClick?: (orderId: string) => void
  }) => (
    <Marker
      coordinates={[cx - 2.5, cy - 2]}
      onClick={() => onClick?.(order.id)}
      style={{
        default: { cursor: onClick ? 'pointer' : 'default' },
        hover: { cursor: onClick ? 'pointer' : 'default' },
      }}
    >
      {/* Área de click invisible más grande */}
      <circle
        r={8}
        fill="transparent"
        stroke="none"
        style={{ cursor: onClick ? 'pointer' : 'default' }}
      />
      <Receipt size={8} />
      <title>{formatTooltipText(order)}</title>
    </Marker>
  ),
)

export default OrderMarker
