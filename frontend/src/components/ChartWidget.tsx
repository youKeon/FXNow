import React, { useState } from 'react';
import { TrendingUp, Calendar } from 'lucide-react';

const ChartWidget: React.FC = () => {
  const [selectedPeriod, setSelectedPeriod] = useState<string>('1M');

  const periods = [
    { value: '1D', label: '1일' },
    { value: '7D', label: '1주' },
    { value: '1M', label: '1개월' },
    { value: '3M', label: '3개월' },
    { value: '1Y', label: '1년' },
  ];

  return (
    <div className="space-y-4">
      {/* 기간 선택 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <Calendar className="h-4 w-4 text-gray-400" />
          <span className="text-sm font-medium text-gray-700">기간</span>
        </div>
        <div className="flex space-x-1">
          {periods.map((period) => (
            <button
              key={period.value}
              onClick={() => setSelectedPeriod(period.value)}
              className={`px-3 py-1 text-xs font-medium rounded transition-colors duration-200 ${
                selectedPeriod === period.value
                  ? 'bg-gray-900 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {period.label}
            </button>
          ))}
        </div>
      </div>

      {/* 차트 플레이스홀더 */}
      <div className="h-48 bg-gray-50 rounded-lg border border-gray-200 flex items-center justify-center">
        <div className="text-center text-gray-500">
          <TrendingUp className="h-8 w-8 mx-auto mb-2 text-gray-400" />
          <p className="text-sm">USD/KRW 차트</p>
          <p className="text-xs mt-1">차트 기능 준비 중...</p>
        </div>
      </div>

      {/* 현재 환율 정보 */}
      <div className="bg-gray-50 rounded-lg p-3">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs text-gray-500">현재 환율</p>
            <p className="text-lg font-semibold text-gray-900">1,335.50 KRW</p>
          </div>
          <div className="text-right">
            <p className="text-xs text-green-600">+0.5%</p>
            <p className="text-xs text-gray-500">오늘</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChartWidget;