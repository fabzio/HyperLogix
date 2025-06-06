import { env } from '@/env'
import { cn } from '@/lib/utils'

interface CommitHashProps {
  className?: string
}

export default function CommitHash({ className }: CommitHashProps) {
  const commitHash = env.VITE_COMMIT_HASH || 'dev'

  return (
    <span className={cn("text-xs text-muted-foreground font-mono", className)}>
      {commitHash}
    </span>
  )
}
