import OrderIcon from '@/components/icons/OrderIcon'
import type { Order } from '@/domain/Order'
import { memo } from 'react'
import { Marker } from 'react-simple-maps'
import { formatTooltipText } from './index'

const OrderMarker = memo(
  ({
    order,
    cx,
    cy,
  }: {
    order: Order
    cx: number
    cy: number
  }) => (
    <Marker coordinates={[cx - 2.5, cy - 2]}>
      <OrderIcon />
      <title>{formatTooltipText(order)}</title>
    </Marker>
  ),
)

export default OrderMarker
