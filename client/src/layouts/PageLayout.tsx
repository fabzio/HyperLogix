import Typography from '@/components/typography'
import type { ReactNode } from '@tanstack/react-router'

interface Props {
  name: string
  children: ReactNode
}
export default function PageLayout({ name, children }: Props) {
  return (
    <div className="z-20">
      <div className="px-6 py-3">
        <Typography variant="h2">{name}</Typography>
      </div>
      <section className="px-3">{children}</section>
    </div>
  )
}
