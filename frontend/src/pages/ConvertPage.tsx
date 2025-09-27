import React, { useState, useEffect } from 'react';
import { ArrowLeftRight, Calculator, TrendingUp, Bell } from 'lucide-react';
import CurrencySelector from '../components/CurrencySelector';
import ChartWidget from '../components/ChartWidget';
import AlertWidget from '../components/AlertWidget';
import { formatNumber } from '../utils/currencies';
import { api } from '../services/api';
import type { ConversionResult } from '../types';

interface ConvertPageProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

// 변환기 컴포넌트 분리
const CurrencyConverter: React.FC = () => {
  const [amount, setAmount] = useState<string>('1.00');
  const [fromCurrency, setFromCurrency] = useState<string>('USD');
  const [toCurrency, setToCurrency] = useState<string>('KRW');
  const [result, setResult] = useState<ConversionResult | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  const handleSwapCurrencies = () => {
    setFromCurrency(toCurrency);
    setToCurrency(fromCurrency);
  };

  const handleConvert = async () => {
    if (!amount || isNaN(Number(amount)) || Number(amount) <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const conversionResult = await api.convertCurrency(Number(amount), fromCurrency, toCurrency);
      setResult(conversionResult);
    } catch (err) {
      // API 실패 시 데모 데이터 사용
      console.warn('API failed, using demo data:', err);

      // 간단한 환율 데모 데이터
      const demoRates: Record<string, Record<string, number>> = {
        'USD': { 'KRW': 1335.50, 'EUR': 0.85, 'JPY': 110.20 },
        'KRW': { 'USD': 0.00075, 'EUR': 0.00064, 'JPY': 0.083 },
        'EUR': { 'USD': 1.17, 'KRW': 1445.20, 'JPY': 129.50 },
        'JPY': { 'USD': 0.009, 'KRW': 12.05, 'EUR': 0.0077 }
      };

      const rate = demoRates[fromCurrency]?.[toCurrency] || 1;
      const convertedAmount = Number(amount) * rate;

      const demoResult: ConversionResult = {
        amount: Number(amount),
        fromCurrency,
        toCurrency,
        rate,
        convertedAmount,
        timestamp: new Date().toISOString()
      };

      setResult(demoResult);
      setError(''); // 에러 메시지 제거
    } finally {
      setIsLoading(false);
    }
  };

  // Auto-convert when any value changes
  useEffect(() => {
    if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
      handleConvert();
    }
  }, [amount, fromCurrency, toCurrency]);

  // Initial conversion on page load
  useEffect(() => {
    handleConvert();
  }, []);

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    // Allow only numbers and decimal point
    if (/^\d*\.?\d*$/.test(value)) {
      setAmount(value);
    }
  };

  return (
    <>
      {/* 가로 레이아웃: Amount, From, To */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-6 items-end mb-8">
        {/* Amount */}
        <div className="md:col-span-1">
          <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-3">
            Amount
          </label>
          <input
            id="amount"
            type="text"
            value={amount}
            onChange={handleAmountChange}
            placeholder="1.00"
            className="w-full px-4 py-4 text-xl font-semibold border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition-all duration-200"
          />
        </div>

        {/* From */}
        <div className="md:col-span-2">
          <CurrencySelector
            value={fromCurrency}
            onChange={setFromCurrency}
            label="From"
          />
        </div>

        {/* To */}
        <div className="md:col-span-2">
          <CurrencySelector
            value={toCurrency}
            onChange={setToCurrency}
            label="To"
          />
        </div>
      </div>

      {/* Swap Button */}
      <div className="flex justify-center mb-8">
        <button
          onClick={handleSwapCurrencies}
          className="p-3 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-900"
        >
          <ArrowLeftRight className="h-5 w-5 text-gray-600" />
        </button>
      </div>

      {/* 변환 결과 - 좌측 하단 */}
      <div className="text-left">
        {result && !error && (
          <div>
            <p className="text-sm text-gray-500 mb-2">
              {formatNumber(result.amount)} {result.fromCurrency} =
            </p>
            <div className="text-4xl font-light text-gray-900 mb-4">
              {formatNumber(result.convertedAmount)} {result.toCurrency}
            </div>
            <p className="text-sm text-gray-500">
              1 {result.fromCurrency} = {formatNumber(result.rate)} {result.toCurrency}
            </p>
            <p className="text-xs text-gray-400 mt-1">
              마지막 업데이트: {new Date(result.timestamp).toLocaleString('ko-KR')}
            </p>
          </div>
        )}

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        {isLoading && (
          <div className="text-gray-500">
            <p className="text-sm">계산 중...</p>
          </div>
        )}
      </div>
    </>
  );
};

const ConvertPage: React.FC<ConvertPageProps> = ({ activeTab, onTabChange }) => {
  const renderTabContent = () => {
    switch (activeTab) {
      case 'convert':
        return <CurrencyConverter />;
      case 'charts':
        return <ChartWidget />;
      case 'alerts':
        return <AlertWidget />;
      default:
        return <CurrencyConverter />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-6 py-16">
        {/* 변환 카드 */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8">
          {/* 탭 네비게이션 */}
          <div className="flex justify-center space-x-8 mb-8 border-b border-gray-200">
            {[
              { id: 'convert', label: '변환' },
              { id: 'charts', label: '차트' },
              { id: 'alerts', label: '알림' }
            ].map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => onTabChange(tab.id)}
                  className={`py-4 px-6 text-sm font-medium border-b-2 transition-colors duration-200 ${
                    isActive
                      ? 'text-gray-900 border-gray-900'
                      : 'text-gray-500 border-transparent hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          {/* 탭 컨텐츠 */}
          {renderTabContent()}
        </div>
      </div>
    </div>
  );
};

export default ConvertPage;