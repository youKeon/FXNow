import type {
  Currency,
  ExchangeRate,
  ConversionResult,
  ExchangeRateChartResponse,
} from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

export const api = {
  // 현재 환율 조회
  async getCurrentRates(): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/current`);
    if (!response.ok) throw new Error('Failed to fetch current rates');
    const result = await response.json();
    return result.data;
  },

  // 특정 통화쌍 환율 조회
  async getExchangeRate(from: string, to: string): Promise<ExchangeRate> {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/${from}/${to}`);
    if (!response.ok) throw new Error('Failed to fetch exchange rate');
    const result = await response.json();
    return result.data;
  },

  // 환율 변환
  async convertCurrency(amount: number, from: string, to: string): Promise<ConversionResult> {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/convert`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ from, to, amount }),
    });
    if (!response.ok) throw new Error('Failed to convert currency');
    const result = await response.json();
    return result.data;
  },

  // 환율 차트 데이터
  async getExchangeHistory(baseCurrency: string, startDate: string, endDate: string): Promise<ExchangeRateChartResponse> {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/chart/${baseCurrency}?startDate=${startDate}&endDate=${endDate}`);
    if (!response.ok) throw new Error('Failed to fetch exchange history');
    const result = await response.json();
    return result.data;
  },

  // 지원 통화 목록
  async getCurrencies(): Promise<Currency[]> {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/currencies`);
    if (!response.ok) throw new Error('Failed to fetch currencies');
    const result = await response.json();
    return result.data;
  },
};
