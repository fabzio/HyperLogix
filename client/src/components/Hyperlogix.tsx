export default function Hyperlogix() {
  return (
    <>
      <span className="bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 bg-clip-text text-transparent animate-gradient">
        HyperLogix
      </span>

      <style>
        {`
          @keyframes gradient {
            0% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
            100% { background-position: 0% 50%; }
          }
          .animate-gradient {
            background-size: 200% 200%;
            animation: gradient 3s ease infinite;
          }
          .emoji-transition {
            display: inline-block;
            transition: opacity 0.3s ease;
          }
          .fade-in {
            opacity: 1;
          }
          .fade-out {
            opacity: 0;
          }
          .metric-card {
            transition: all 0.3s ease;
          }
          .metric-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
          }
          .dark .metric-card:hover {
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5);
          }
        `}
      </style>
    </>
  )
}
