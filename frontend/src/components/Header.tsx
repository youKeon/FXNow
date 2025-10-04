import React, { useState, useEffect } from 'react';
import { Globe, Zap } from 'lucide-react';
import FXIcon from './FXIcon';
import { api } from '../services/api';
import { formatNumber } from '../utils/currencies';

interface LiveRate {
  currency: string;
  rate: string;
  change: string;
  isPositive: boolean;
}

const Header: React.FC = () => {
  const [liveRates, setLiveRates] = useState<LiveRate[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchLiveRates = async () => {
      try {
        const currentRates = await api.getCurrentRates();

        // API 응답을 LiveRate 형식으로 변환
        const formattedRates: LiveRate[] = Object.entries(currentRates).map(([, value]: [string, any]) => ({
          currency: `${value.baseCurrency}/${value.targetCurrency}`,
          rate: formatNumber(value.rate),
          change: `${value.change >= 0 ? '+' : ''}${value.changePercent.toFixed(2)}%`,
          isPositive: value.change >= 0,
        }));

        // USD, EUR, JPY만 표시 (상위 3개)
        const displayRates = formattedRates
          .filter(r => r.currency.startsWith('USD/') || r.currency.startsWith('EUR/') || r.currency.startsWith('JPY/'))
          .slice(0, 3);

        setLiveRates(displayRates);
      } catch (error) {
        console.error('Failed to fetch live rates:', error);
        // 폴백 데이터 (API 실패 시)
        setLiveRates([
          { currency: 'USD/KRW', rate: '—', change: '—', isPositive: true },
          { currency: 'EUR/KRW', rate: '—', change: '—', isPositive: true },
          { currency: 'JPY/KRW', rate: '—', change: '—', isPositive: true },
        ]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchLiveRates();

    // 30초마다 갱신
    const interval = setInterval(fetchLiveRates, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 border-b border-gray-700 shadow-lg">
      <div className="px-4">
        {/* 상단: 로고와 LIVE 표시 */}
        <div className="flex items-center justify-between h-14">
          <div className="flex items-center space-x-2">
            <FXIcon size={32} />
            <div>
              <h1 className="text-lg font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                FXNow
              </h1>
            </div>
          </div>

          {/* Live 표시 */}
          <div className="flex items-center space-x-2 px-2 py-1 bg-green-500 bg-opacity-20 rounded-full border border-green-500 border-opacity-30">
            <div className="relative">
              <div className="w-1.5 h-1.5 bg-green-400 rounded-full"></div>
              <div className="absolute inset-0 w-1.5 h-1.5 bg-green-400 rounded-full animate-ping opacity-75"></div>
            </div>
            <Zap className="h-3 w-3 text-green-400" />
            <span className="text-xs font-semibold text-green-400">LIVE</span>
          </div>
        </div>

        {/* 하단: 주요 환율 정보 (모바일 최적화) */}
        <div className="pb-3 -mx-4 px-4">
          <div className="flex space-x-2 overflow-x-auto scrollbar-hide">
            {isLoading ? (
              // 로딩 스켈레톤
              <>
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex-shrink-0 p-2 bg-gray-800 bg-opacity-50 rounded-lg border border-gray-700 min-w-[100px] animate-pulse">
                    <div className="h-3 bg-gray-700 rounded w-16 mb-2" />
                    <div className="h-4 bg-gray-700 rounded w-20 mb-2" />
                    <div className="h-4 bg-gray-700 rounded w-12" />
                  </div>
                ))}
              </>
            ) : (
              liveRates.map((rate, index) => (
                <div key={index} className="flex-shrink-0 p-2 bg-gray-800 bg-opacity-50 rounded-lg border border-gray-700 min-w-[100px]">
                  <div className="flex items-center space-x-1 mb-1">
                    <Globe className="h-2.5 w-2.5 text-gray-400" />
                    <div className="text-xs font-medium text-gray-300">{rate.currency}</div>
                  </div>
                  <div className="text-sm font-bold text-white mb-1">{rate.rate}</div>
                  <span className={`text-xs font-semibold px-1.5 py-0.5 rounded-full ${
                    rate.isPositive
                      ? 'text-green-300 bg-green-500 bg-opacity-20'
                      : 'text-red-300 bg-red-500 bg-opacity-20'
                  }`}>
                    {rate.change}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;