import React, { useState } from 'react';
import { Bell, Plus, Trash2 } from 'lucide-react';

interface Alert {
  id: string;
  pair: string;
  targetRate: number;
  currentRate: number;
  isAbove: boolean;
}

const AlertWidget: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([
    {
      id: '1',
      pair: 'USD/KRW',
      targetRate: 1400,
      currentRate: 1335.50,
      isAbove: true,
    },
  ]);
  const [showForm, setShowForm] = useState(false);
  const [targetRate, setTargetRate] = useState('');

  const handleAddAlert = () => {
    if (!targetRate || isNaN(Number(targetRate))) return;

    const newAlert: Alert = {
      id: Date.now().toString(),
      pair: 'USD/KRW',
      targetRate: Number(targetRate),
      currentRate: 1335.50,
      isAbove: Number(targetRate) > 1335.50,
    };

    setAlerts([...alerts, newAlert]);
    setTargetRate('');
    setShowForm(false);
  };

  const handleDeleteAlert = (id: string) => {
    setAlerts(alerts.filter(alert => alert.id !== id));
  };

  return (
    <div className="space-y-4">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <Bell className="h-4 w-4 text-gray-400" />
          <span className="text-sm font-medium text-gray-700">환율 알림</span>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
        >
          <Plus className="h-4 w-4" />
        </button>
      </div>

      {/* 알림 추가 폼 */}
      {showForm && (
        <div className="bg-gray-50 rounded-lg p-3 space-y-3">
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">
              목표 환율 (KRW)
            </label>
            <input
              type="number"
              value={targetRate}
              onChange={(e) => setTargetRate(e.target.value)}
              placeholder="1400"
              className="w-full px-3 py-2 text-sm border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-gray-900"
            />
          </div>
          <div className="flex space-x-2">
            <button
              onClick={handleAddAlert}
              className="flex-1 py-2 px-3 text-xs bg-gray-900 text-white rounded hover:bg-gray-800 transition-colors"
            >
              추가
            </button>
            <button
              onClick={() => setShowForm(false)}
              className="flex-1 py-2 px-3 text-xs bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition-colors"
            >
              취소
            </button>
          </div>
        </div>
      )}

      {/* 알림 목록 */}
      <div className="space-y-2 max-h-32 overflow-y-auto">
        {alerts.length === 0 ? (
          <div className="text-center py-6 text-gray-500">
            <Bell className="h-6 w-6 mx-auto mb-2 text-gray-400" />
            <p className="text-xs">설정된 알림이 없습니다</p>
          </div>
        ) : (
          alerts.map((alert) => (
            <div key={alert.id} className="bg-gray-50 rounded-lg p-3 flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">{alert.pair}</p>
                <p className="text-xs text-gray-500">
                  {alert.targetRate.toLocaleString()} KRW {alert.isAbove ? '이상' : '이하'}
                </p>
              </div>
              <div className="flex items-center space-x-2">
                <span className={`text-xs px-2 py-1 rounded-full ${
                  alert.isAbove && alert.currentRate >= alert.targetRate
                    ? 'bg-green-100 text-green-700'
                    : !alert.isAbove && alert.currentRate <= alert.targetRate
                    ? 'bg-green-100 text-green-700'
                    : 'bg-gray-100 text-gray-600'
                }`}>
                  {alert.isAbove && alert.currentRate >= alert.targetRate
                    ? '도달'
                    : !alert.isAbove && alert.currentRate <= alert.targetRate
                    ? '도달'
                    : '대기'}
                </span>
                <button
                  onClick={() => handleDeleteAlert(alert.id)}
                  className="p-1 text-gray-400 hover:text-red-500 transition-colors"
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* 알림 설명 */}
      <div className="bg-blue-50 rounded-lg p-2">
        <p className="text-xs text-blue-700">
          💡 실제 서비스에서는 이메일이나 푸시 알림으로 전송됩니다.
        </p>
      </div>
    </div>
  );
};

export default AlertWidget;