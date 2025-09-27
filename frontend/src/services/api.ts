import type { Currency, ExchangeRate, ConversionResult, ChartDataPoint } from '../types';

const API_BASE_URL = 'http://localhost:8080/api/v1';

export const api = {
  // 환율 조회
  async getExchangeRate(from: string, to: string): Promise<ExchangeRate> {
    const response = await fetch(`${API_BASE_URL}/exchange/rates?from=${from}&to=${to}`);
    if (!response.ok) throw new Error('Failed to fetch exchange rate');
    return response.json();
  },

  // 환율 변환
  async convertCurrency(amount: number, from: string, to: string): Promise<ConversionResult> {
    const response = await fetch(`${API_BASE_URL}/exchange/convert`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ amount, fromCurrency: from, toCurrency: to }),
    });
    if (!response.ok) throw new Error('Failed to convert currency');
    return response.json();
  },

  // 환율 히스토리
  async getExchangeHistory(from: string, to: string, period: string): Promise<ChartDataPoint[]> {
    const response = await fetch(`${API_BASE_URL}/exchange/history?from=${from}&to=${to}&period=${period}`);
    if (!response.ok) throw new Error('Failed to fetch exchange history');
    const data = await response.json();
    return data.data;
  },

  // 지원 통화 목록
  async getCurrencies(): Promise<Currency[]> {
    const response = await fetch(`${API_BASE_URL}/currencies`);
    if (!response.ok) throw new Error('Failed to fetch currencies');
    const data = await response.json();
    return data.data;
  },
};