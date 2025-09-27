import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Calendar, TrendingUp, TrendingDown } from 'lucide-react';
import CurrencySelector from '../components/CurrencySelector';
import { api } from '../services/api';
import { formatNumber } from '../utils/currencies';
import type { ChartDataPoint, TimePeriod } from '../types';

interface ChartsPageProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

const ChartsPage: React.FC<ChartsPageProps> = ({ activeTab, onTabChange }) => {
  const [fromCurrency, setFromCurrency] = useState<string>('USD');
  const [toCurrency, setToCurrency] = useState<string>('KRW');
  const [selectedPeriod, setSelectedPeriod] = useState<TimePeriod>('1M');
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  const periods: { value: TimePeriod; label: string }[] = [
    { value: '1D', label: '1일' },
    { value: '7D', label: '1주' },
    { value: '1M', label: '1개월' },
    { value: '3M', label: '3개월' },
    { value: '1Y', label: '1년' },
    { value: '5Y', label: '5년' },
  ];

  const fetchChartData = async () => {
    setIsLoading(true);
    setError('');

    try {
      const data = await api.getExchangeHistory(fromCurrency, toCurrency, selectedPeriod);
      setChartData(data);
    } catch (err) {
      setError('Failed to fetch chart data. Please try again.');
      console.error('Chart data error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchChartData();
  }, [fromCurrency, toCurrency, selectedPeriod]);

  const calculateChange = () => {
    if (chartData.length < 2) return { value: 0, percentage: 0, isPositive: false };

    const first = chartData[0].rate;
    const last = chartData[chartData.length - 1].rate;
    const change = last - first;
    const percentage = (change / first) * 100;

    return {
      value: change,
      percentage: Math.abs(percentage),
      isPositive: change >= 0,
    };
  };

  const change = calculateChange();
  const currentRate = chartData.length > 0 ? chartData[chartData.length - 1].rate : 0;

  const formatTooltipLabel = (label: string) => {
    const date = new Date(label);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: selectedPeriod === '1D' ? 'numeric' : undefined,
      minute: selectedPeriod === '1D' ? 'numeric' : undefined,
    });
  };

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white border border-gray-300 rounded-lg p-3 shadow-lg">
          <p className="text-sm font-medium text-gray-900">
            {formatTooltipLabel(label)}
          </p>
          <p className="text-sm text-xe-blue">
            {`1 ${fromCurrency} = ${formatNumber(payload[0].value)} ${toCurrency}`}
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">환율 차트</h1>
        <p className="text-gray-600">환율 변화 추이를 시각적으로 확인하세요</p>
      </div>

      {/* Currency Selection */}
      <div className="bg-white rounded-2xl shadow-lg p-6 mb-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <CurrencySelector
            value={fromCurrency}
            onChange={setFromCurrency}
            label="Base Currency"
          />
          <CurrencySelector
            value={toCurrency}
            onChange={setToCurrency}
            label="Quote Currency"
          />
        </div>
      </div>

      {/* Current Rate and Change */}
      {chartData.length > 0 && (
        <div className="bg-white rounded-2xl shadow-lg p-6 mb-8">
          <div className="text-center">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              {fromCurrency}/{toCurrency}
            </h2>
            <div className="flex items-center justify-center space-x-6 mb-4">
              <div>
                <p className="text-3xl font-bold text-gray-900">
                  {formatNumber(currentRate)}
                </p>
                <p className="text-sm text-gray-500">
                  1 {fromCurrency} = {formatNumber(currentRate)} {toCurrency}
                </p>
              </div>
              <div className={`flex items-center space-x-2 ${
                change.isPositive ? 'text-green-600' : 'text-red-600'
              }`}>
                {change.isPositive ? (
                  <TrendingUp className="h-6 w-6" />
                ) : (
                  <TrendingDown className="h-6 w-6" />
                )}
                <div className="text-right">
                  <p className="text-lg font-semibold">
                    {change.isPositive ? '+' : ''}{formatNumber(change.value)}
                  </p>
                  <p className="text-sm">
                    ({change.isPositive ? '+' : ''}{change.percentage.toFixed(2)}%)
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Period Selection */}
      <div className="bg-white rounded-2xl shadow-lg p-6 mb-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-2">
            <Calendar className="h-5 w-5 text-gray-400" />
            <span className="text-sm font-medium text-gray-700">기간 선택</span>
          </div>
          <div className="flex space-x-2">
            {periods.map((period) => (
              <button
                key={period.value}
                onClick={() => setSelectedPeriod(period.value)}
                className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors duration-200 ${
                  selectedPeriod === period.value
                    ? 'bg-xe-blue text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {period.label}
              </button>
            ))}
          </div>
        </div>

        {/* Chart */}
        <div className="h-96">
          {isLoading ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-gray-500">Loading chart data...</div>
            </div>
          ) : error ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <p className="text-red-600 mb-2">{error}</p>
                <button
                  onClick={fetchChartData}
                  className="px-4 py-2 bg-xe-blue text-white rounded-lg hover:opacity-90 transition-opacity duration-200"
                >
                  다시 시도
                </button>
              </div>
            </div>
          ) : chartData.length === 0 ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-gray-500">No data available</div>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => {
                    const date = new Date(value);
                    if (selectedPeriod === '1D') {
                      return date.toLocaleTimeString('ko-KR', {
                        hour: '2-digit',
                        minute: '2-digit'
                      });
                    }
                    return date.toLocaleDateString('ko-KR', {
                      month: 'short',
                      day: 'numeric'
                    });
                  }}
                />
                <YAxis
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => formatNumber(value)}
                />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone"
                  dataKey="rate"
                  stroke="#005DAB"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4, stroke: '#005DAB', strokeWidth: 2 }}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Chart Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <div className="text-blue-600 mt-0.5">
            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
            </svg>
          </div>
          <div>
            <p className="text-sm text-blue-800 font-medium">환율 차트 정보</p>
            <p className="text-xs text-blue-700 mt-1">
              차트는 실시간 중간시장환율을 기반으로 합니다. 실제 거래 환율과는 차이가 있을 수 있습니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChartsPage;