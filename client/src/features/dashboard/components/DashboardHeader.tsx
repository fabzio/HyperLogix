import { GradientBackground } from '@/components/GradientBackground'
import Hyperlogix from '@/components/Hyperlogix'
import Typography from '@/components/typography'
import { useEffect, useState } from 'react'

interface Props {
  currentTime: Date
}

export function DashboardHeader({ currentTime }: Props) {
  const [emojiIndex, setEmojiIndex] = useState(0)
  const [fadeState, setFadeState] = useState('fade-in')

  const techEmojis = ['⛽', '🛢️', '🚒', '🧪', '🔥', '💨', '🚛', '📍', '⏱️', '🔄']

  useEffect(() => {
    const emojiTimer = setInterval(() => {
      // Start the fade out
      setFadeState('fade-out')

      // After fade out completes, change emoji and fade in
      setTimeout(() => {
        setEmojiIndex((prevIndex) => (prevIndex + 1) % techEmojis.length)
        setFadeState('fade-in')
      }, 300) // This should match the transition duration in CSS
    }, 3000) // Change emoji every 3 seconds

    return () => {
      clearInterval(emojiTimer)
    }
  }, [])

  return (
    <>
      <GradientBackground />

      <div className="flex flex-col md:flex-row justify-between items-center mb-8 relative z-10">
        <Typography variant="h1" className="flex">
          <Hyperlogix />
          <span className={`emoji-transition ${fadeState}`}>
            {techEmojis[emojiIndex]}
          </span>
        </Typography>

        <div className="text-right mt-4 md:mt-0">
          <div className="text-3xl font-bold tracking-wider text-cyan-500 dark:text-cyan-400">
            {formatTime(currentTime)}
          </div>
          <div className="text-muted-foreground">{formatDate(currentTime)}</div>
        </div>
      </div>
    </>
  )
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('es-ES', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

const formatDate = (date: Date) => {
  return date.toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}
