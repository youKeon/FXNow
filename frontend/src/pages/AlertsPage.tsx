import React, { useState, useEffect } from 'react';
import { Plus, Bell, BellOff, Trash2, Edit3 } from 'lucide-react';
import CurrencySelector from '../components/CurrencySelector';
import { formatNumber } from '../utils/currencies';
import type { Alert } from '../types';

interface AlertsPageProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

const AlertsPage: React.FC<AlertsPageProps> = ({ activeTab, onTabChange }) => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [showCreateForm, setShowCreateForm] = useState<boolean>(false);
  const [editingAlert, setEditingAlert] = useState<Alert | null>(null);

  // Create/Edit form states
  const [fromCurrency, setFromCurrency] = useState<string>('USD');
  const [toCurrency, setToCurrency] = useState<string>('KRW');
  const [targetRate, setTargetRate] = useState<string>('');

  useEffect(() => {
    // Load alerts from localStorage
    const savedAlerts = localStorage.getItem('fxnow-alerts');
    if (savedAlerts) {
      setAlerts(JSON.parse(savedAlerts));
    }
  }, []);

  useEffect(() => {
    // Save alerts to localStorage
    localStorage.setItem('fxnow-alerts', JSON.stringify(alerts));
  }, [alerts]);

  const handleCreateAlert = () => {
    if (!targetRate || isNaN(Number(targetRate)) || Number(targetRate) <= 0) {
      alert('목표 환율을 올바르게 입력해주세요.');
      return;
    }

    const newAlert: Alert = {
      id: Date.now().toString(),
      fromCurrency,
      toCurrency,
      targetRate: Number(targetRate),
      currentRate: 0, // This would be fetched from API
      isActive: true,
      createdAt: new Date().toISOString(),
    };

    setAlerts([...alerts, newAlert]);
    resetForm();
  };

  const handleEditAlert = () => {
    if (!editingAlert || !targetRate || isNaN(Number(targetRate)) || Number(targetRate) <= 0) {
      alert('목표 환율을 올바르게 입력해주세요.');
      return;
    }

    setAlerts(alerts.map(alert =>
      alert.id === editingAlert.id
        ? { ...alert, targetRate: Number(targetRate) }
        : alert
    ));
    resetForm();
  };

  const handleDeleteAlert = (id: string) => {
    if (confirm('정말로 이 알림을 삭제하시겠습니까?')) {
      setAlerts(alerts.filter(alert => alert.id !== id));
    }
  };

  const handleToggleAlert = (id: string) => {
    setAlerts(alerts.map(alert =>
      alert.id === id ? { ...alert, isActive: !alert.isActive } : alert
    ));
  };

  const startEdit = (alert: Alert) => {
    setEditingAlert(alert);
    setFromCurrency(alert.fromCurrency);
    setToCurrency(alert.toCurrency);
    setTargetRate(alert.targetRate.toString());
    setShowCreateForm(true);
  };

  const resetForm = () => {
    setShowCreateForm(false);
    setEditingAlert(null);
    setFromCurrency('USD');
    setToCurrency('KRW');
    setTargetRate('');
  };

  const activeAlerts = alerts.filter(alert => alert.isActive);
  const inactiveAlerts = alerts.filter(alert => !alert.isActive);

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">환율 알림</h1>
        <p className="text-gray-600">목표 환율에 도달하면 알림을 받으세요</p>
      </div>

      {/* Create Alert Button */}
      {!showCreateForm && (
        <div className="text-center mb-8">
          <button
            onClick={() => setShowCreateForm(true)}
            className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-xe-blue to-xe-green text-white font-semibold rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-xe-blue focus:ring-offset-2 transition-opacity duration-200"
          >
            <Plus className="h-5 w-5 mr-2" />
            새 알림 만들기
          </button>
        </div>
      )}

      {/* Create/Edit Alert Form */}
      {showCreateForm && (
        <div className="bg-white rounded-2xl shadow-lg p-8 mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">
            {editingAlert ? '알림 수정' : '새 알림 만들기'}
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <CurrencySelector
              value={fromCurrency}
              onChange={setFromCurrency}
              label="From Currency"
            />
            <CurrencySelector
              value={toCurrency}
              onChange={setToCurrency}
              label="To Currency"
            />
          </div>

          <div className="mb-6">
            <label htmlFor="targetRate" className="block text-sm font-medium text-gray-700 mb-2">
              목표 환율
            </label>
            <div className="relative">
              <input
                id="targetRate"
                type="text"
                value={targetRate}
                onChange={(e) => {
                  const value = e.target.value;
                  if (/^\d*\.?\d*$/.test(value)) {
                    setTargetRate(value);
                  }
                }}
                placeholder="목표 환율을 입력하세요"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-xe-blue focus:border-transparent"
              />
              <div className="absolute inset-y-0 right-0 flex items-center pr-3">
                <span className="text-sm text-gray-500">
                  1 {fromCurrency} = ? {toCurrency}
                </span>
              </div>
            </div>
          </div>

          <div className="flex space-x-4">
            <button
              onClick={editingAlert ? handleEditAlert : handleCreateAlert}
              className="flex-1 py-3 bg-xe-blue text-white font-semibold rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-xe-blue focus:ring-offset-2 transition-opacity duration-200"
            >
              {editingAlert ? '수정하기' : '알림 만들기'}
            </button>
            <button
              onClick={resetForm}
              className="flex-1 py-3 bg-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors duration-200"
            >
              취소
            </button>
          </div>
        </div>
      )}

      {/* Active Alerts */}
      {activeAlerts.length > 0 && (
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center">
            <Bell className="h-5 w-5 mr-2 text-xe-green" />
            활성 알림 ({activeAlerts.length})
          </h2>
          <div className="space-y-4">
            {activeAlerts.map((alert) => (
              <div key={alert.id} className="bg-white rounded-xl shadow-lg border border-green-200 p-6">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <span className="text-lg font-semibold text-gray-900">
                        {alert.fromCurrency}/{alert.toCurrency}
                      </span>
                      <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-medium rounded-full">
                        활성
                      </span>
                    </div>
                    <div className="text-sm text-gray-600">
                      <p>목표 환율: <span className="font-medium">{formatNumber(alert.targetRate)}</span></p>
                      <p>생성일: {new Date(alert.createdAt).toLocaleDateString('ko-KR')}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => startEdit(alert)}
                      className="p-2 text-gray-400 hover:text-xe-blue rounded-lg hover:bg-gray-100 transition-colors duration-200"
                      title="수정"
                    >
                      <Edit3 className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleToggleAlert(alert.id)}
                      className="p-2 text-green-600 hover:text-green-800 rounded-lg hover:bg-green-50 transition-colors duration-200"
                      title="비활성화"
                    >
                      <Bell className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteAlert(alert.id)}
                      className="p-2 text-red-600 hover:text-red-800 rounded-lg hover:bg-red-50 transition-colors duration-200"
                      title="삭제"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Inactive Alerts */}
      {inactiveAlerts.length > 0 && (
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center">
            <BellOff className="h-5 w-5 mr-2 text-gray-400" />
            비활성 알림 ({inactiveAlerts.length})
          </h2>
          <div className="space-y-4">
            {inactiveAlerts.map((alert) => (
              <div key={alert.id} className="bg-white rounded-xl shadow-lg border border-gray-200 p-6 opacity-75">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <span className="text-lg font-semibold text-gray-900">
                        {alert.fromCurrency}/{alert.toCurrency}
                      </span>
                      <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs font-medium rounded-full">
                        비활성
                      </span>
                    </div>
                    <div className="text-sm text-gray-600">
                      <p>목표 환율: <span className="font-medium">{formatNumber(alert.targetRate)}</span></p>
                      <p>생성일: {new Date(alert.createdAt).toLocaleDateString('ko-KR')}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => startEdit(alert)}
                      className="p-2 text-gray-400 hover:text-xe-blue rounded-lg hover:bg-gray-100 transition-colors duration-200"
                      title="수정"
                    >
                      <Edit3 className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleToggleAlert(alert.id)}
                      className="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100 transition-colors duration-200"
                      title="활성화"
                    >
                      <BellOff className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteAlert(alert.id)}
                      className="p-2 text-red-600 hover:text-red-800 rounded-lg hover:bg-red-50 transition-colors duration-200"
                      title="삭제"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {alerts.length === 0 && !showCreateForm && (
        <div className="text-center py-12">
          <div className="mx-auto w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-4">
            <Bell className="h-12 w-12 text-gray-400" />
          </div>
          <h3 className="text-xl font-medium text-gray-900 mb-2">알림이 없습니다</h3>
          <p className="text-gray-500 mb-6">
            환율 알림을 설정하여 목표 환율에 도달했을 때 알림을 받으세요.
          </p>
          <button
            onClick={() => setShowCreateForm(true)}
            className="inline-flex items-center px-6 py-3 bg-xe-blue text-white font-semibold rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-xe-blue focus:ring-offset-2 transition-opacity duration-200"
          >
            <Plus className="h-5 w-5 mr-2" />
            첫 번째 알림 만들기
          </button>
        </div>
      )}

      {/* Alert Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <div className="text-blue-600 mt-0.5">
            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
            </svg>
          </div>
          <div>
            <p className="text-sm text-blue-800 font-medium">알림 기능 안내</p>
            <p className="text-xs text-blue-700 mt-1">
              현재 데모 버전에서는 브라우저의 로컬 스토리지에 알림이 저장됩니다.
              실제 서비스에서는 서버에서 실시간 환율을 모니터링하여 이메일 또는 푸시 알림을 보냅니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AlertsPage;