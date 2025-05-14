import { useRouterState } from '@tanstack/react-router'
import { useEffect, useRef } from 'react'
import LoadingBar, { type LoadingBarRef } from 'react-top-loading-bar'

interface Props {
  children: React.ReactNode
}
export default function LoadingBarProvider({ children }: Props) {
  const routerState = useRouterState()
  const ref = useRef<LoadingBarRef>(null)
  useEffect(() => {
    if (!ref.current) return
    if (routerState.status === 'pending') {
      ref.current.continuousStart()
    } else {
      ref.current.complete()
    }
  }, [routerState.status])
  return (
    <>
      <LoadingBar ref={ref} color="#2998ff" />
      {children}
    </>
  )
}
