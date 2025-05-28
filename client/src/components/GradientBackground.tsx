import { useEffect, useState } from 'react'

interface GradientBackgroundProps {
  className?: string
  intensity?: number // 0-1 value for opacity
  blurAmount?: number // in pixels
}

export function GradientBackground({
  className = '',
  intensity = 0.5,
  blurAmount = 100,
}: GradientBackgroundProps) {
  const [isAnimating, setIsAnimating] = useState(false)

  // Initialize animation on component mount
  useEffect(() => {
    setIsAnimating(true)
  }, [])

  return (
    <>
      <div className={`blur-spots ${className}`}>
        <div
          className={`blur-spot blur-spot-1 ${isAnimating ? 'animate' : ''}`}
        />
        <div
          className={`blur-spot blur-spot-2 ${isAnimating ? 'animate' : ''}`}
        />
        <div
          className={`blur-spot blur-spot-3 ${isAnimating ? 'animate' : ''}`}
        />
        <div
          className={`blur-spot blur-spot-4 ${isAnimating ? 'animate' : ''}`}
        />
        <div
          className={`blur-spot blur-spot-5 ${isAnimating ? 'animate' : ''}`}
        />
      </div>

      <style>{`
        .blur-spots {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          overflow: hidden;
          z-index: 0;
          pointer-events: none;
        }
        
        .blur-spot {
          position: absolute;
          border-radius: 50%;
          filter: blur(${blurAmount}px);
          opacity: 0;
          mix-blend-mode: plus-lighter;
          transition: opacity 1s ease-in;
        }
        
        /* Different blend mode and opacity for dark/light modes */
        @media (prefers-color-scheme: dark) {
          .blur-spot {
            mix-blend-mode: lighten;
            opacity: 0;
          }
          
          .blur-spot.animate {
            opacity: ${intensity * 1.2};
            filter: blur(${blurAmount * 0.9}px) saturate(1.4);
          }
        }
        
        @media (prefers-color-scheme: light) {
          .blur-spot {
            mix-blend-mode: soft-light;
            opacity: 0;
          }
          
          .blur-spot.animate {
            opacity: ${intensity * 0.3};
            filter: blur(${blurAmount * 1.2}px) saturate(0.8);
          }
        }
        
        .blur-spot.animate {
          animation-play-state: running !important;
          opacity: ${intensity * 0.8}; /* Default fallback */
        }
        
        /* Multiple traveling spots - wavy, diffused gradients */
        .blur-spot-1 {
          width: 28vw;
          height: 24vw;
          border-radius: 65% 35% 55% 45% / 40% 60% 40% 60%;
          background: 
            conic-gradient(
              from 90deg at 60% 40%,
              rgba(59, 130, 246, ${intensity * 0.9}) 0deg,
              rgba(139, 92, 246, ${intensity * 0.7}) 120deg,
              rgba(56, 189, 248, ${intensity * 0.6}) 240deg,
              rgba(59, 130, 246, ${intensity * 0.9}) 360deg
            ),
            radial-gradient(
              ellipse at 30% 70%,
              rgba(56, 189, 248, ${intensity * 0.7}) 0%, 
              rgba(192, 38, 211, ${intensity * 0.6}) 60%, 
              transparent 90%
            );
          background-blend-mode: screen;
          top: 5%;
          left: 8%;
          animation: lava-motion-1 40s ease-in-out infinite;
          filter: blur(${blurAmount * 1.1}px);
        }
        
        .blur-spot-2 {
          width: 26vw;
          height: 28vw;
          border-radius: 40% 60% 30% 70% / 60% 30% 70% 40%;
          background: 
            conic-gradient(
              from 0deg at 40% 60%,
              rgba(3, 105, 161, ${intensity * 0.8}) 0deg,
              rgba(168, 85, 247, ${intensity * 0.7}) 150deg,
              rgba(59, 130, 246, ${intensity * 0.6}) 300deg,
              rgba(3, 105, 161, ${intensity * 0.8}) 360deg
            ),
            radial-gradient(
              ellipse at 70% 40%,
              rgba(79, 70, 229, ${intensity * 0.7}) 0%, 
              rgba(217, 70, 239, ${intensity * 0.5}) 70%, 
              transparent 90%
            );
          background-blend-mode: screen;
          bottom: 5%;
          right: 8%;
          animation: lava-motion-2 45s ease-in-out infinite;
          filter: blur(${blurAmount * 1.1}px);
        }
        
        .blur-spot-3 {
          width: 23vw;
          height: 21vw;
          border-radius: 55% 45% 40% 60% / 35% 65% 35% 65%;
          background: 
            conic-gradient(
              from 180deg at 50% 50%,
              rgba(14, 165, 233, ${intensity * 0.7}) 0deg,
              rgba(125, 211, 252, ${intensity * 0.8}) 90deg,
              rgba(139, 92, 246, ${intensity * 0.6}) 180deg,
              rgba(96, 165, 250, ${intensity * 0.7}) 270deg,
              rgba(14, 165, 233, ${intensity * 0.7}) 360deg
            ),
            radial-gradient(
              ellipse at 35% 65%,
              rgba(56, 189, 248, ${intensity * 0.8}) 0%, 
              rgba(168, 85, 247, ${intensity * 0.5}) 60%, 
              transparent 90%
            );
          background-blend-mode: screen;
          top: 65%;
          left: 35%;
          animation: lava-motion-3 42s ease-in-out infinite;
          filter: blur(${blurAmount * 1.1}px);
        }
        
        .blur-spot-4 {
          width: 22vw;
          height: 19vw;
          border-radius: 50% 50% 35% 65% / 55% 45% 65% 35%;
          background: 
            conic-gradient(
              from 270deg at 35% 65%,
              rgba(96, 165, 250, ${intensity * 0.7}) 0deg,
              rgba(59, 130, 246, ${intensity * 0.8}) 120deg,
              rgba(139, 92, 246, ${intensity * 0.6}) 240deg,
              rgba(96, 165, 250, ${intensity * 0.7}) 360deg
            ),
            radial-gradient(
              ellipse at 65% 35%,
              rgba(14, 165, 233, ${intensity * 0.8}) 0%, 
              rgba(168, 85, 247, ${intensity * 0.5}) 60%, 
              transparent 90%
            );
          background-blend-mode: screen;
          top: 15%;
          right: 25%;
          animation: lava-motion-4 38s ease-in-out infinite;
          filter: blur(${blurAmount * 1.1}px);
        }
        
        .blur-spot-5 {
          width: 24vw;
          height: 22vw;
          border-radius: 60% 40% 50% 50% / 40% 60% 50% 50%;
          background: 
            conic-gradient(
              from 45deg at 60% 40%,
              rgba(37, 99, 235, ${intensity * 0.7}) 0deg,
              rgba(3, 105, 161, ${intensity * 0.8}) 90deg,
              rgba(139, 92, 246, ${intensity * 0.6}) 180deg,
              rgba(125, 211, 252, ${intensity * 0.7}) 270deg,
              rgba(37, 99, 235, ${intensity * 0.7}) 360deg
            ),
            radial-gradient(
              ellipse at 40% 60%,
              rgba(56, 189, 248, ${intensity * 0.7}) 0%, 
              rgba(168, 85, 247, ${intensity * 0.5}) 70%, 
              transparent 90%
            );
          background-blend-mode: screen;
          top: 40%;
          left: 15%;
          animation: lava-motion-5 48s ease-in-out infinite;
          filter: blur(${blurAmount * 1.1}px);
        }
        
        /* Animation keyframes for traveling spots */
        @keyframes lava-motion-1 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          10% { transform: translate(15%, 5%) scale(1.05); }
          25% { transform: translate(40%, 15%) scale(0.95); }
          40% { transform: translate(60%, 30%) scale(1.02); }
          55% { transform: translate(35%, 45%) scale(0.98); }
          70% { transform: translate(10%, 30%) scale(1.03); }
          85% { transform: translate(-15%, 10%) scale(0.97); }
        }
        
        @keyframes lava-motion-2 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          15% { transform: translate(-20%, -10%) scale(1.04); }
          30% { transform: translate(-45%, -25%) scale(0.96); }
          45% { transform: translate(-35%, -45%) scale(1.02); }
          60% { transform: translate(-10%, -35%) scale(0.98); }
          75% { transform: translate(15%, -20%) scale(1.03); }
          90% { transform: translate(10%, -5%) scale(0.97); }
        }
        
        @keyframes lava-motion-3 {
          0%, 100% { transform: translate(0, 0) scale(1) rotate(0deg); }
          12% { transform: translate(15%, 15%) scale(1.03) rotate(2deg); }
          24% { transform: translate(5%, 30%) scale(0.97) rotate(0deg); }
          36% { transform: translate(-20%, 20%) scale(1.02) rotate(-2deg); }
          48% { transform: translate(-35%, 0%) scale(0.98) rotate(-3deg); }
          60% { transform: translate(-25%, -25%) scale(1.01) rotate(-1deg); }
          72% { transform: translate(0%, -35%) scale(0.99) rotate(1deg); }
          84% { transform: translate(20%, -15%) scale(1.02) rotate(3deg); }
        }
        
        @keyframes lava-motion-4 {
          0%, 100% { transform: translate(0, 0) scale(1) rotate(0deg); }
          16% { transform: translate(-15%, 15%) scale(1.04) rotate(3deg); }
          32% { transform: translate(-30%, 35%) scale(0.96) rotate(1deg); }
          48% { transform: translate(-10%, 45%) scale(1.02) rotate(-2deg); }
          64% { transform: translate(20%, 30%) scale(0.98) rotate(-4deg); }
          80% { transform: translate(35%, 10%) scale(1.03) rotate(0deg); }
        }
        
        @keyframes lava-motion-5 {
          0%, 100% { transform: translate(0, 0) scale(1) rotate(0deg); }
          14% { transform: translate(20%, -15%) scale(1.03) rotate(2deg); }
          28% { transform: translate(35%, -5%) scale(0.97) rotate(4deg); }
          42% { transform: translate(25%, 25%) scale(1.01) rotate(0deg); }
          56% { transform: translate(0%, 35%) scale(0.99) rotate(-3deg); }
          70% { transform: translate(-25%, 20%) scale(1.02) rotate(-5deg); }
          84% { transform: translate(-15%, -5%) scale(0.98) rotate(-2deg); }
        }
        
        .glassmorphism {
          background: rgba(255, 255, 255, 0.08) !important;
          backdrop-filter: blur(10px) !important;
          border: 1px solid rgba(255, 255, 255, 0.1) !important;
          box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.12) !important;
        }
        
        .dark .glassmorphism {
          background: rgba(17, 25, 40, 0.6) !important;
          border: 1px solid rgba(255, 255, 255, 0.05) !important;
        }
      `}</style>
    </>
  )
}
