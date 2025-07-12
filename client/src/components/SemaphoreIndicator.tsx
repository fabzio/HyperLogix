import { cn } from '@/lib/utils'
import { type VariantProps, cva } from 'class-variance-authority'

const semaphoreVariants = cva(
  'inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors',
  {
    variants: {
      status: {
        excellent:
          'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800/50',
        good: 'bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-900/20 dark:text-blue-400 dark:border-blue-800/50',
        warning:
          'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-400 dark:border-yellow-800/50',
        danger:
          'bg-red-100 text-red-800 border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-800/50',
        critical:
          'bg-red-200 text-red-900 border-red-300 dark:bg-red-900/40 dark:text-red-300 dark:border-red-700/50',
      },
      size: {
        default: 'h-6 px-2.5 py-0.5',
        sm: 'h-5 px-2 py-0.5 text-xs',
        lg: 'h-7 px-3 py-1',
        indicator: 'h-3 w-3 rounded-full p-0', // Just a dot indicator
      },
    },
    defaultVariants: {
      status: 'good',
      size: 'default',
    },
  },
)

interface SemaphoreIndicatorProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof semaphoreVariants> {
  value: number
  maxValue: number
  thresholds?: {
    excellent: number // >= this percentage
    good: number // >= this percentage
    warning: number // >= this percentage
    danger: number // >= this percentage
    // < danger = critical
  }
  inverse?: boolean // true for cases where lower values are better (like remaining time)
  showAsIndicator?: boolean // just show colored dot
}

export function SemaphoreIndicator({
  value,
  maxValue,
  thresholds = {
    excellent: 80,
    good: 60,
    warning: 40,
    danger: 20,
  },
  inverse = false,
  showAsIndicator = false,
  size,
  className,
  ...props
}: SemaphoreIndicatorProps) {
  const percentage = maxValue > 0 ? (value / maxValue) * 100 : 0
  const effectivePercentage = inverse ? 100 - percentage : percentage

  let status: 'excellent' | 'good' | 'warning' | 'danger' | 'critical'

  if (effectivePercentage >= thresholds.excellent) {
    status = 'excellent'
  } else if (effectivePercentage >= thresholds.good) {
    status = 'good'
  } else if (effectivePercentage >= thresholds.warning) {
    status = 'warning'
  } else if (effectivePercentage >= thresholds.danger) {
    status = 'danger'
  } else {
    status = 'critical'
  }

  const effectiveSize = showAsIndicator ? 'indicator' : size

  return (
    <div
      className={cn(
        semaphoreVariants({ status, size: effectiveSize }),
        showAsIndicator ? 'border-2' : 'border',
        className,
      )}
      title={`${percentage.toFixed(1)}%`}
      {...props}
    />
  )
}
