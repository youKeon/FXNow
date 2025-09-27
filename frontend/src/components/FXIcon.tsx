interface FXIconProps {
  className?: string;
  size?: number;
}

export default function FXIcon({ className = "", size = 32 }: FXIconProps) {
  return (
    <svg 
      width={size} 
      height={size} 
      viewBox="0 0 32 32" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      {/* Gradient Background Circle for Dark Theme */}
      <defs>
        <linearGradient id="darkGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#4CAF50" />
          <stop offset="100%" stopColor="#007AFF" />
        </linearGradient>
      </defs>
      <circle cx="16" cy="16" r="16" fill="url(#darkGradient)"/>
      
      {/* Dollar Sign - Brighter for Dark Theme */}
      <path 
        d="M14 8V10M14 22V24M14 10C12.5 10 11.5 11 11.5 12.5C11.5 14 12.5 15 14 15H16C17.5 15 18.5 16 18.5 17.5C18.5 19 17.5 20 16 20H12M14 10H18M14 20V22" 
        stroke="#ffffff" 
        strokeWidth="1.8" 
        strokeLinecap="round" 
        strokeLinejoin="round"
      />
      
      {/* Exchange Arrows - Enhanced for Dark Theme */}
      <path 
        d="M22 11L24 13L22 15M24 13H20" 
        stroke="#ffffff" 
        strokeWidth="1.5" 
        strokeLinecap="round" 
        strokeLinejoin="round"
        opacity="0.9"
      />
      <path 
        d="M10 21L8 19L10 17M8 19H12" 
        stroke="#ffffff" 
        strokeWidth="1.5" 
        strokeLinecap="round" 
        strokeLinejoin="round"
        opacity="0.9"
      />
    </svg>
  );
}