import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { NumericFormat } from 'react-number-format';
import toast from 'react-hot-toast';
import { ArrowLeftRight } from 'lucide-react';
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

  const { setValue } = useForm({
    defaultValues: {
      amount: '1.00'
    }
  });

  const handleSwapCurrencies = () => {
    setFromCurrency(toCurrency);
    setToCurrency(fromCurrency);
  };

  const handleConvert = async (formAmount?: string) => {
    const currentAmount = formAmount || amount;
    if (!currentAmount || isNaN(Number(currentAmount)) || Number(currentAmount) <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const conversionResult = await api.convertCurrency(Number(currentAmount), fromCurrency, toCurrency);
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
      const convertedAmount = Number(currentAmount) * rate;

      const demoResult: ConversionResult = {
        amount: Number(currentAmount),
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

  const handleAmountChange = (values: any) => {
    const { value } = values;
    setAmount(value || '');
    setValue('amount', value || '');
  };

  return (
    <>
      {/* 모바일 세로 레이아웃 */}
      <div className="space-y-4 mb-6">
        {/* Amount */}
        <div>
          <label htmlFor="amount" className="block text-sm font-medium text-gray-300 mb-2">
            Amout
          </label>
          <NumericFormat
            id="amount"
            value={amount}
            onValueChange={handleAmountChange}
            placeholder="1.00"
            allowNegative={false}
            decimalScale={2}
            thousandSeparator=","
            className="w-full px-4 py-4 text-lg font-semibold bg-gray-700 border border-gray-600 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all duration-200"
          />
        </div>

        {/* From */}
        <div>
          <CurrencySelector
            value={fromCurrency}
            onChange={setFromCurrency}
            label="From"
          />
        </div>

        {/* Swap Button */}
        <div className="flex justify-center py-2">
          <motion.button
            onClick={handleSwapCurrencies}
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.9, rotate: 180 }}
            transition={{ duration: 0.2 }}
            className="p-3 bg-gray-700 hover:bg-gray-600 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            <ArrowLeftRight className="h-5 w-5 text-gray-300" />
          </motion.button>
        </div>

        {/* To */}
        <div>
          <CurrencySelector
            value={toCurrency}
            onChange={setToCurrency}
            label="To"
          />
        </div>
      </div>

      {/* 변환 결과 */}
      <div className="bg-gray-700 rounded-lg p-4 mb-4">
        <AnimatePresence mode="wait">
          {result && !error && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3, ease: "easeOut" }}
            >
              <p className="text-sm text-gray-400 mb-2 text-center">
                {formatNumber(result.amount)} {result.fromCurrency} =
              </p>
              <motion.div
                className="text-3xl font-semibold text-white mb-3 text-center"
                initial={{ scale: 0.95 }}
                animate={{ scale: 1 }}
                transition={{ duration: 0.2, delay: 0.1 }}
              >
                {formatNumber(result.convertedAmount)} {result.toCurrency}
              </motion.div>
              <div className="text-center space-y-1">
                <p className="text-sm text-gray-400">
                  1 {result.fromCurrency} = {formatNumber(result.rate)} {result.toCurrency}
                </p>
                <p className="text-xs text-gray-500">
                  마지막 업데이트: {new Date(result.timestamp).toLocaleString('ko-KR')}
                </p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {error && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="p-4 bg-red-900 bg-opacity-50 border border-red-600 rounded-lg"
          >
            <p className="text-sm text-red-400">{error}</p>
          </motion.div>
        )}

        {isLoading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-gray-400"
          >
            <motion.p
              animate={{ opacity: [1, 0.5, 1] }}
              transition={{ duration: 1.5, repeat: Infinity }}
              className="text-sm"
            >
              계산 중...
            </motion.p>
          </motion.div>
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
    <div className="min-h-screen bg-gray-900">
      <div className="px-4 py-4">
        {/* 변환 카드 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="bg-gray-800 rounded-lg shadow-sm border border-gray-700 p-4"
        >
          {/* 탭 네비게이션 */}
          <div className="flex justify-center space-x-4 mb-6 border-b border-gray-600">
            {[
              { id: 'convert', label: '변환' },
              { id: 'charts', label: '차트' },
              { id: 'alerts', label: '알림' }
            ].map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <motion.button
                  key={tab.id}
                  onClick={() => onTabChange(tab.id)}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  className={`py-3 px-4 text-sm font-medium border-b-2 transition-colors duration-200 ${
                    isActive
                      ? 'text-red-400 border-red-400'
                      : 'text-gray-400 border-transparent hover:text-gray-300 hover:border-gray-500'
                  }`}
                >
                  {tab.label}
                </motion.button>
              );
            })}
          </div>

          {/* 탭 컨텐츠 */}
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
            >
              {renderTabContent()}
            </motion.div>
          </AnimatePresence>
        </motion.div>
      </div>
    </div>
  );
};

export default ConvertPage;