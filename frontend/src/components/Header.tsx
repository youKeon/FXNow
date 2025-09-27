import React from 'react';

const Header: React.FC = () => {
  // 실시간 환율 데이터 (실제로는 API에서 가져와야 함)
  const liveRates = [
    { currency: 'USD/KRW', rate: '1,335.50', change: '+0.75%', isPositive: true },
    { currency: 'EUR/KRW', rate: '1,445.20', change: '-0.23%', isPositive: false },
    { currency: 'JPY/KRW', rate: '8.95', change: '+0.12%', isPositive: true },
  ];

  return (
    <header className="bg-white border-b border-gray-200">
      <div className="max-w-6xl mx-auto px-6">
        <div className="flex items-center justify-between h-16">
          {/* 로고 영역 */}
          <div className="flex items-center">
            <h1 className="text-xl font-semibold text-gray-900">
              FXNow
            </h1>
          </div>

          {/* 실시간 환율 정보 */}
          <div className="flex items-center space-x-8">
            {/* Live 표시 */}
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              <span className="text-sm font-medium text-gray-600">LIVE</span>
            </div>

            {/* 환율 정보 */}
            <div className="hidden md:flex items-center space-x-8">
              {liveRates.map((rate, index) => (
                <div key={index} className="text-right">
                  <div className="text-xs text-gray-500">{rate.currency}</div>
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{rate.rate}</span>
                    <span className={`text-xs font-medium ${
                      rate.isPositive ? 'text-green-600' : 'text-red-600'
                    }`}>
                      {rate.change}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* 모바일용 간단 표시 */}
            <div className="md:hidden text-right">
              <div className="text-xs text-gray-500">USD/KRW</div>
              <div className="flex items-center space-x-2">
                <span className="text-sm font-medium text-gray-900">1,335.50</span>
                <span className="text-xs font-medium text-green-600">+0.75%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;