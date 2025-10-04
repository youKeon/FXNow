/**
 * 환율 변환기 관련 상수
 */
export const CURRENCY_CONFIG = {
  /** Debounce 시간 (ms) */
  DEBOUNCE_MS: 500,

  /** 소수점 자릿수 */
  DECIMAL_SCALE: 2,

  /** 기본 금액 */
  DEFAULT_AMOUNT: '1.00',

  /** 기본 출발 통화 */
  DEFAULT_FROM: 'USD',

  /** 기본 도착 통화 */
  DEFAULT_TO: 'KRW',
} as const;

/**
 * 데모 모드 환율 데이터
 * API 실패 시 사용되는 폴백 데이터
 */
export const DEMO_RATES: Record<string, Record<string, number>> = {
  'USD': { 'KRW': 1335.50, 'EUR': 0.85, 'JPY': 110.20 },
  'KRW': { 'USD': 0.00075, 'EUR': 0.00064, 'JPY': 0.083 },
  'EUR': { 'USD': 1.17, 'KRW': 1445.20, 'JPY': 129.50 },
  'JPY': { 'USD': 0.009, 'KRW': 12.05, 'EUR': 0.0077 }
};

/**
 * 접근성(Accessibility) 라벨
 */
export const ARIA_LABELS = {
  AMOUNT_INPUT: '변환할 금액 입력',
  SWAP_CURRENCIES: '통화 교환',
  REFRESH_RATE: '최신 환율 조회',
  CLICK_TO_SWAP: '결과를 클릭하여 통화 교환'
} as const;
