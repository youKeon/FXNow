export interface Currency {
  code: string;
  name: string;
  symbol: string;
  flag: string;
}

export interface ExchangeRate {
  from: string;
  to: string;
  rate: number;
  timestamp: string;
  change24h?: number;
  changePercent24h?: number;
}

export interface ConversionResult {
  amount: number;
  fromCurrency: string;
  toCurrency: string;
  convertedAmount: number;
  rate: number;
  timestamp: string;
}

export interface ChartDataPoint {
  date: string;
  rate: number;
}

export interface Alert {
  id: string;
  fromCurrency: string;
  toCurrency: string;
  targetRate: number;
  currentRate: number;
  isActive: boolean;
  createdAt: string;
  triggeredAt?: string;
}

export type TimePeriod = '1D' | '7D' | '1M' | '3M' | '1Y' | '5Y';