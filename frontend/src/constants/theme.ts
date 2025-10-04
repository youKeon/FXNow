/**
 * FXNow 디자인 시스템 테마
 * 일관된 UI를 위한 색상, 간격, 타이포그래피 상수
 */

export const THEME = {
  // 배경색
  background: {
    primary: 'bg-gray-900',
    secondary: 'bg-gray-800',
    tertiary: 'bg-gray-700',
    card: 'bg-gray-800',
    cardHover: 'bg-gray-700',
    input: 'bg-gray-700',
    inputFocus: 'bg-gray-600',
  },

  // 텍스트 색상
  text: {
    primary: 'text-white',
    secondary: 'text-gray-300',
    tertiary: 'text-gray-400',
    muted: 'text-gray-500',
    success: 'text-green-400',
    error: 'text-red-400',
    warning: 'text-yellow-400',
  },

  // 테두리
  border: {
    default: 'border-gray-700',
    hover: 'border-gray-600',
    focus: 'border-cyan-400',
    input: 'border-gray-600',
  },

  // 버튼 (Primary - Red)
  button: {
    primary: {
      bg: 'bg-red-500',
      hover: 'bg-red-600',
      active: 'bg-red-700',
      text: 'text-white',
      disabled: 'bg-gray-600 text-gray-400 cursor-not-allowed',
    },
    secondary: {
      bg: 'bg-gray-600',
      hover: 'bg-gray-500',
      text: 'text-gray-300',
    },
    ghost: {
      bg: 'bg-transparent',
      hover: 'bg-gray-700',
      text: 'text-gray-300',
    },
  },

  // 포커스 링
  focus: {
    ring: 'focus:outline-none focus:ring-2 focus:ring-red-500',
    ringOffset: 'focus:ring-offset-2 focus:ring-offset-gray-900',
    ringBlue: 'focus:ring-cyan-400',
  },

  // 상태 색상
  status: {
    success: {
      bg: 'bg-green-500 bg-opacity-20',
      border: 'border-green-500 border-opacity-30',
      text: 'text-green-400',
    },
    error: {
      bg: 'bg-red-500 bg-opacity-20',
      border: 'border-red-500 border-opacity-30',
      text: 'text-red-400',
    },
    warning: {
      bg: 'bg-yellow-500 bg-opacity-20',
      border: 'border-yellow-500 border-opacity-30',
      text: 'text-yellow-400',
    },
    info: {
      bg: 'bg-blue-500 bg-opacity-20',
      border: 'border-blue-500 border-opacity-30',
      text: 'text-blue-400',
    },
  },

  // 그림자
  shadow: {
    sm: 'shadow-sm',
    md: 'shadow-md',
    lg: 'shadow-lg',
    xl: 'shadow-xl',
  },

  // 반응형 간격
  spacing: {
    card: 'p-6',
    section: 'space-y-6',
    element: 'space-y-4',
  },

  // 트랜지션
  transition: {
    default: 'transition-all duration-200',
    fast: 'transition-all duration-150',
    slow: 'transition-all duration-300',
  },

  // 차트 색상
  chart: {
    line: '#EF4444', // red-500
    high: '#22d3ee', // cyan-400
    low: '#a855f7', // purple-500
    grid: 'rgba(148, 163, 184, 0.15)',
    tooltip: {
      bg: 'bg-gray-800',
      border: 'border-gray-600',
    },
  },
} as const;

/**
 * 포커스 링 생성 헬퍼
 */
export const getFocusRing = (offset = true): string => {
  return offset
    ? `${THEME.focus.ring} ${THEME.focus.ringOffset}`
    : THEME.focus.ring;
};

/**
 * 버튼 클래스 생성 헬퍼
 */
export const getButtonClasses = (
  variant: 'primary' | 'secondary' | 'ghost' = 'primary',
  disabled = false
): string => {
  const base = 'px-4 py-2 rounded-lg font-medium transition-colors duration-200';
  const focus = getFocusRing();

  if (disabled) {
    return `${base} ${THEME.button.primary.disabled} ${focus}`;
  }

  const styles = {
    primary: `${THEME.button.primary.bg} hover:${THEME.button.primary.hover} ${THEME.button.primary.text}`,
    secondary: `${THEME.button.secondary.bg} hover:${THEME.button.secondary.hover} ${THEME.button.secondary.text}`,
    ghost: `${THEME.button.ghost.bg} hover:${THEME.button.ghost.hover} ${THEME.button.ghost.text}`,
  };

  return `${base} ${styles[variant]} ${focus}`;
};

/**
 * 카드 클래스 생성 헬퍼
 */
export const getCardClasses = (hover = false): string => {
  const base = `${THEME.background.card} ${THEME.border.default} border rounded-lg ${THEME.spacing.card}`;
  const hoverEffect = hover ? `hover:${THEME.border.hover} cursor-pointer` : '';
  return `${base} ${hoverEffect} ${THEME.transition.default}`;
};

/**
 * 입력 필드 클래스 생성 헬퍼
 */
export const getInputClasses = (): string => {
  return `w-full px-4 py-3 ${THEME.background.input} ${THEME.border.input} border ${THEME.text.primary} rounded-lg ${getFocusRing(false)} focus:${THEME.border.focus} focus:${THEME.background.inputFocus} ${THEME.transition.default}`;
};
