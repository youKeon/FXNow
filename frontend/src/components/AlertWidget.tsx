import React, { useState } from 'react';
import { Bell, Plus, Trash2, TrendingUp, TrendingDown } from 'lucide-react';

type AlertType = 'absolute' | 'percentage' | 'threshold';
type ThresholdDirection = 'above' | 'below';

interface Alert {
  id: string;
  pair: string;
  type: AlertType;
  targetRate?: number;
  percentage?: number;
  baseRate?: number;
  thresholdRate?: number;
  direction?: ThresholdDirection;
  currentRate: number;
  isActive: boolean;
}

const AlertWidget: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([
    {
      id: '1',
      pair: 'USD/KRW',
      type: 'absolute',
      targetRate: 1400,
      currentRate: 1335.50,
      isActive: true,
    },
  ]);
  const [showForm, setShowForm] = useState(false);
  const [alertType, setAlertType] = useState<AlertType>('absolute');
  const [targetRate, setTargetRate] = useState('');
  const [percentage, setPercentage] = useState('');
  const [thresholdRate, setThresholdRate] = useState('');
  const [direction, setDirection] = useState<ThresholdDirection>('above');

  const handleAddAlert = () => {
    const currentRate = 1335.50;
    let newAlert: Alert;

    if (alertType === 'absolute') {
      if (!targetRate || isNaN(Number(targetRate))) return;
      newAlert = {
        id: Date.now().toString(),
        pair: 'USD/KRW',
        type: 'absolute',
        targetRate: Number(targetRate),
        currentRate,
        isActive: true,
      };
    } else if (alertType === 'percentage') {
      if (!percentage || isNaN(Number(percentage))) return;
      newAlert = {
        id: Date.now().toString(),
        pair: 'USD/KRW',
        type: 'percentage',
        percentage: Number(percentage),
        baseRate: currentRate,
        currentRate,
        isActive: true,
      };
    } else { // threshold
      if (!thresholdRate || isNaN(Number(thresholdRate))) return;
      newAlert = {
        id: Date.now().toString(),
        pair: 'USD/KRW',
        type: 'threshold',
        thresholdRate: Number(thresholdRate),
        direction,
        currentRate,
        isActive: true,
      };
    }

    setAlerts([...alerts, newAlert]);
    setTargetRate('');
    setPercentage('');
    setThresholdRate('');
    setShowForm(false);
  };

  const handleDeleteAlert = (id: string) => {
    setAlerts(alerts.filter(alert => alert.id !== id));
  };

  const handleToggleAlert = (id: string) => {
    setAlerts(alerts.map(alert =>
      alert.id === id ? { ...alert, isActive: !alert.isActive } : alert
    ));
  };

  const getAlertStatus = (alert: Alert): { status: 'triggered' | 'waiting'; text: string } => {
    const { currentRate, type } = alert;

    if (type === 'absolute') {
      const isTriggered = currentRate >= alert.targetRate!;
      return {
        status: isTriggered ? 'triggered' : 'waiting',
        text: isTriggered ? '도달' : '대기'
      };
    } else if (type === 'percentage') {
      const targetRate = alert.baseRate! * (1 + alert.percentage! / 100);
      const isTriggered = currentRate >= targetRate;
      return {
        status: isTriggered ? 'triggered' : 'waiting',
        text: isTriggered ? '도달' : '대기'
      };
    } else { // threshold
      const isTriggered = alert.direction === 'above'
        ? currentRate >= alert.thresholdRate!
        : currentRate <= alert.thresholdRate!;
      return {
        status: isTriggered ? 'triggered' : 'waiting',
        text: isTriggered ? '도달' : '대기'
      };
    }
  };

  const getAlertDescription = (alert: Alert): string => {
    if (alert.type === 'absolute') {
      return `${alert.targetRate!.toLocaleString()} KRW 도달 시`;
    } else if (alert.type === 'percentage') {
      const targetRate = alert.baseRate! * (1 + alert.percentage! / 100);
      return `${alert.percentage!}% 상승 (${targetRate.toLocaleString()} KRW)`;
    } else {
      return `${alert.thresholdRate!.toLocaleString()} KRW ${alert.direction === 'above' ? '이상' : '이하'}`;
    }
  };

  return (
    <div className="space-y-4">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <Bell className="h-4 w-4 text-gray-400" />
          <span className="text-sm font-medium text-gray-300">환율 알림</span>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="p-2 bg-red-500 hover:bg-red-600 rounded-lg transition-colors"
        >
          <Plus className="h-4 w-4 text-white" />
        </button>
      </div>

      {/* 알림 추가 폼 */}
      {showForm && (
        <div className="bg-gray-700 rounded-lg p-4 space-y-4">
          {/* 알림 타입 선택 */}
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              알림 타입
            </label>
            <div className="grid grid-cols-3 gap-2">
              <button
                onClick={() => setAlertType('absolute')}
                className={`p-2 text-xs rounded-lg transition-colors ${
                  alertType === 'absolute'
                    ? 'bg-red-500 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                절대값
              </button>
              <button
                onClick={() => setAlertType('percentage')}
                className={`p-2 text-xs rounded-lg transition-colors ${
                  alertType === 'percentage'
                    ? 'bg-red-500 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                퍼센트
              </button>
              <button
                onClick={() => setAlertType('threshold')}
                className={`p-2 text-xs rounded-lg transition-colors ${
                  alertType === 'threshold'
                    ? 'bg-red-500 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                임계값
              </button>
            </div>
          </div>

          {/* 입력 필드 */}
          {alertType === 'absolute' && (
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                목표 환율 (KRW)
              </label>
              <input
                type="number"
                value={targetRate}
                onChange={(e) => setTargetRate(e.target.value)}
                placeholder="1450"
                className="w-full px-3 py-2 text-sm bg-gray-600 border border-gray-500 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
              />
              <p className="text-xs text-gray-400 mt-1">예: 1450원 도달 시 알림</p>
            </div>
          )}

          {alertType === 'percentage' && (
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                변동률 (%)
              </label>
              <input
                type="number"
                value={percentage}
                onChange={(e) => setPercentage(e.target.value)}
                placeholder="3"
                className="w-full px-3 py-2 text-sm bg-gray-600 border border-gray-500 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
              />
              <p className="text-xs text-gray-400 mt-1">현재가 기준 3% 상승 시 알림</p>
            </div>
          )}

          {alertType === 'threshold' && (
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  임계값 (KRW)
                </label>
                <input
                  type="number"
                  value={thresholdRate}
                  onChange={(e) => setThresholdRate(e.target.value)}
                  placeholder="1450"
                  className="w-full px-3 py-2 text-sm bg-gray-600 border border-gray-500 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  조건
                </label>
                <div className="flex space-x-2">
                  <button
                    onClick={() => setDirection('above')}
                    className={`flex-1 flex items-center justify-center space-x-1 p-2 text-xs rounded-lg transition-colors ${
                      direction === 'above'
                        ? 'bg-green-500 text-white'
                        : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                    }`}
                  >
                    <TrendingUp className="h-3 w-3" />
                    <span>이상</span>
                  </button>
                  <button
                    onClick={() => setDirection('below')}
                    className={`flex-1 flex items-center justify-center space-x-1 p-2 text-xs rounded-lg transition-colors ${
                      direction === 'below'
                        ? 'bg-red-500 text-white'
                        : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                    }`}
                  >
                    <TrendingDown className="h-3 w-3" />
                    <span>이하</span>
                  </button>
                </div>
              </div>
            </div>
          )}

          <div className="flex space-x-2">
            <button
              onClick={handleAddAlert}
              className="flex-1 py-2 px-3 text-sm bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
            >
              알림 추가
            </button>
            <button
              onClick={() => setShowForm(false)}
              className="flex-1 py-2 px-3 text-sm bg-gray-600 text-gray-300 rounded-lg hover:bg-gray-500 transition-colors"
            >
              취소
            </button>
          </div>
        </div>
      )}

      {/* 알림 목록 */}
      <div className="space-y-3">
        {alerts.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            <Bell className="h-8 w-8 mx-auto mb-3 text-gray-500" />
            <p className="text-sm">설정된 알림이 없습니다</p>
            <p className="text-xs text-gray-500 mt-1">위의 + 버튼을 눌러 알림을 추가하세요</p>
          </div>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {alerts.map((alert) => {
              const alertStatus = getAlertStatus(alert);
              return (
                <div key={alert.id} className={`bg-gray-700 rounded-lg p-3 space-y-2 ${
                  !alert.isActive ? 'opacity-60' : ''
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm font-medium text-white">{alert.pair}</span>
                      {alert.isActive && alertStatus.status === 'triggered' && (
                        <span className="text-xs px-2 py-1 rounded-full bg-green-500 bg-opacity-20 text-green-400">
                          도달
                        </span>
                      )}
                    </div>
                    <div className="flex items-center space-x-3">
                      {/* 토글 스위치 */}
                      <div className="flex items-center space-x-2">
                        <span className={`text-xs font-medium ${
                          alert.isActive ? 'text-green-400' : 'text-gray-500'
                        }`}>
                          {alert.isActive ? 'ON' : 'OFF'}
                        </span>
                        <button
                          onClick={() => handleToggleAlert(alert.id)}
                          className={`relative inline-flex h-6 w-11 items-center rounded-full transition-all duration-200 border-2 ${
                            alert.isActive
                              ? 'bg-green-500 border-green-400 shadow-lg shadow-green-500/30'
                              : 'bg-gray-600 border-gray-500'
                          }`}
                        >
                          <span className={`inline-block h-4 w-4 transform rounded-full transition-all duration-200 ${
                            alert.isActive
                              ? 'translate-x-5 bg-white shadow-md'
                              : 'translate-x-1 bg-gray-300'
                          }`} />
                        </button>
                      </div>
                      <button
                        onClick={() => handleDeleteAlert(alert.id)}
                        className="p-1 text-gray-400 hover:text-red-400 transition-colors"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">
                      {getAlertDescription(alert)}
                    </p>
                    <p className="text-xs text-gray-500 mt-1">
                      현재: {alert.currentRate.toLocaleString()} KRW
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 알림 설명 */}
      <div className="bg-blue-500 bg-opacity-20 border border-blue-500 border-opacity-30 rounded-lg p-3">
        <p className="text-xs text-blue-300">
          💡 실제 서비스에서는 이메일이나 푸시 알림으로 전송됩니다.
        </p>
      </div>
    </div>
  );
};

export default AlertWidget;