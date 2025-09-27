import React, { useState, useMemo } from 'react';
import { TrendingUp, Calendar } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const ChartWidget: React.FC = () => {
  const [selectedPeriod, setSelectedPeriod] = useState<string>('1M');

  const periods = [
    { value: '1D', label: '1일' },
    { value: '7D', label: '1주' },
    { value: '1M', label: '1개월' },
    { value: '3M', label: '3개월' },
    { value: '1Y', label: '1년' },
  ];

  // 더미 차트 데이터 생성
  const chartData = useMemo(() => {
    const generateData = () => {
      const baseRate = 1335.50;
      const data = [];
      const now = new Date();

      if (selectedPeriod === '1D') {
        // 1일 차트: 시간 단위로 24개 데이터 포인트 생성
        for (let i = 24; i >= 0; i--) {
          const date = new Date(now);
          date.setHours(date.getHours() - i);

          // 랜덤 변동을 추가하여 현실적인 환율 차트 생성
          const variation = (Math.random() - 0.5) * 20; // ±10원 변동 (작은 변동)
          const rate = baseRate + variation + (Math.sin(i * 0.2) * 10); // 시간대별 트렌드

          data.push({
            date: date.toISOString(),
            rate: Math.round(rate * 100) / 100,
            displayDate: date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
          });
        }
      } else {
        // 다른 기간들: 기존 로직 유지
        let days = 30;
        if (selectedPeriod === '7D') days = 7;
        else if (selectedPeriod === '3M') days = 90;
        else if (selectedPeriod === '1Y') days = 365;

        for (let i = days; i >= 0; i--) {
          const date = new Date(now);
          date.setDate(date.getDate() - i);

          // 랜덤 변동을 추가하여 현실적인 환율 차트 생성
          const variation = (Math.random() - 0.5) * 50; // ±25원 변동
          const rate = baseRate + variation + (Math.sin(i * 0.1) * 20); // 약간의 트렌드 추가

          data.push({
            date: date.toISOString(),
            rate: Math.round(rate * 100) / 100,
            displayDate: date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
          });
        }
      }

      return data;
    };

    return generateData();
  }, [selectedPeriod]);

  const currentRate = chartData[chartData.length - 1]?.rate || 1335.50;
  const previousRate = chartData[chartData.length - 2]?.rate || 1335.50;
  const change = currentRate - previousRate;
  const changePercent = (change / previousRate) * 100;

  return (
    <div className="space-y-4">
      {/* 기간 선택 */}
      <div className="space-y-3">
        <div className="flex items-center space-x-2">
          <Calendar className="h-4 w-4 text-gray-400" />
          <span className="text-sm font-medium text-gray-300">기간 선택</span>
        </div>
        <div className="grid grid-cols-5 gap-2">
          {periods.map((period) => (
            <button
              key={period.value}
              onClick={() => setSelectedPeriod(period.value)}
              className={`px-2 py-2 text-xs font-medium rounded transition-colors duration-200 ${
                selectedPeriod === period.value
                  ? 'bg-red-500 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              {period.label}
            </button>
          ))}
        </div>
      </div>

      {/* 실제 차트 */}
      <div className="h-64 bg-gray-900 rounded-lg border border-gray-700 p-2">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={chartData}
            margin={{ top: 5, right: 5, left: 5, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
            <XAxis
              dataKey="displayDate"
              tick={{ fontSize: 10, fill: '#9CA3AF' }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tick={{ fontSize: 10, fill: '#9CA3AF' }}
              axisLine={false}
              tickLine={false}
              domain={['dataMin - 10', 'dataMax + 10']}
              tickFormatter={(value) => `${value.toFixed(0)}`}
            />
            <Tooltip
              content={({ active, payload, label }) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="bg-gray-800 border border-gray-600 rounded-lg p-2 shadow-lg">
                      <p className="text-xs font-medium text-gray-200">{label}</p>
                      <p className="text-xs text-red-400">
                        1 USD = {payload[0].value?.toFixed(2)} KRW
                      </p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <Line
              type="monotone"
              dataKey="rate"
              stroke="#EF4444"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4, stroke: '#EF4444', strokeWidth: 2, fill: '#EF4444' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>


      {/* 현재 환율 정보 */}
      <div className="bg-gray-800 rounded-lg p-4">
        <div className="text-center space-y-2">
          <p className="text-sm text-gray-400">현재 환율 (USD/KRW)</p>
          <p className="text-2xl font-bold text-white">
            {currentRate.toLocaleString('ko-KR', { minimumFractionDigits: 2 })} KRW
          </p>
          <div className="flex items-center justify-center space-x-2">
            <p className={`text-sm font-semibold ${change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {change >= 0 ? '+' : ''}{changePercent.toFixed(2)}%
            </p>
            <span className="text-gray-500">•</span>
            <p className="text-sm text-gray-400">
              {selectedPeriod === '1D' ? '시간대비' : selectedPeriod === '7D' ? '주간대비' : '기간대비'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChartWidget;