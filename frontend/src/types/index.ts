export interface Currency {
  code: string;
  name: string;
  flag: string;
  symbol?: string;
}

export interface ExchangeRate {
  pair: string;
  rate: number;
  timestamp: string;
}

export interface ConversionResult {
  amount: number;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  convertedAmount: number;
  timestamp: string;
}

export interface ChartDataPoint {
  date: string;
  rate: number;
}

export type TimePeriod = '1D' | '7D' | '1M' | '3M' | '1Y' | '5Y';

export interface Alert {
  id: string;
  fromCurrency: string;
  toCurrency: string;
  targetRate: number;
  currentRate: number;
  isActive: boolean;
  createdAt: string;
}