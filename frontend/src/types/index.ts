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
  convertedAmount: number;
  from: string;
  to: string;
  amount: number;
  rate: number;
  timestamp: string;
}

export interface ChartDataPoint {
  date: string;
  time?: string | null;
  rate: number;
  dayChange: number;
}

export interface ChartStatistics {
  high: number;
  low: number;
  average: number;
}

export interface ExchangeRateChartResponse {
  baseCurrency: string;
  targetCurrency: string;
  period: string;
  currentRate: number;
  change: number;
  changePercent: number;
  lastUpdated: string;
  chartData: ChartDataPoint[];
  statistics: ChartStatistics;
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
