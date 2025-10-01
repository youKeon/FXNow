import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { NumericFormat } from 'react-number-format';
import { ArrowLeftRight, RefreshCw, TrendingUp, TrendingDown } from 'lucide-react';
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
  const [lastUpdate, setLastUpdate] = useState<string>('');
  const [previousRate, setPreviousRate] = useState<number | null>(null);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const { setValue } = useForm({
    defaultValues: {
      amount: '1.00'
    }
  });

  const handleSwapCurrencies = () => {
    const newFromCurrency = toCurrency;
    const newToCurrency = fromCurrency;

    setFromCurrency(newFromCurrency);
    setToCurrency(newToCurrency);

    // 통화 변경 즉시 새로운 환율로 계산
    if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
      handleConvert(amount);
    }
  };

  const handleConvert = async (formAmount?: string) => {
    const currentAmount = formAmount || amount;
    if (!currentAmount || isNaN(Number(currentAmount)) || Number(currentAmount) <= 0) {
      setError('');
      setResult(null);
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const apiResponse = await api.convertCurrency(Number(currentAmount), fromCurrency, toCurrency);

      // API 응답에 요청 정보 추가
      const conversionResult: ConversionResult = {
        converted_amount: apiResponse.converted_amount,
        rate: apiResponse.rate,
        timestamp: apiResponse.timestamp,
        from: fromCurrency,
        to: toCurrency,
        amount: Number(currentAmount)
      };

      // API 응답 검증
      if (conversionResult &&
          !isNaN(conversionResult.converted_amount) &&
          isFinite(conversionResult.converted_amount) &&
          !isNaN(conversionResult.rate) &&
          isFinite(conversionResult.rate)) {
        setPreviousRate(result?.rate || null);
        setResult(conversionResult);
        setLastUpdate(new Date(conversionResult.timestamp).toLocaleString('ko-KR'));
      }
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

      // 계산 결과가 유효한 경우에만 설정
      if (!isNaN(convertedAmount) && isFinite(convertedAmount) &&
          !isNaN(rate) && isFinite(rate)) {
        setPreviousRate(result?.rate || null);
        const demoResult: ConversionResult = {
          converted_amount: convertedAmount,
          from: fromCurrency,
          to: toCurrency,
          amount: Number(currentAmount),
          rate,
          timestamp: new Date().toISOString()
        };

        setResult(demoResult);
        setLastUpdate(new Date().toLocaleString('ko-KR'));
      }
      setError(''); // 에러 메시지 제거
    } finally {
      setIsLoading(false);
    }
  };

  // Auto-convert when currency changes (not amount - handled by debounce)
  useEffect(() => {
    if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
      handleConvert();
    }
  }, [fromCurrency, toCurrency]);

  // Initial conversion on page load
  useEffect(() => {
    // 초기 로드 시 즉시 API 호출하여 환율 가져오기
    const initConvert = async () => {
      if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
        await handleConvert(amount);
      }
    };
    initConvert();
  }, []);

  // 디바운스 타이머
  const [debounceTimer, setDebounceTimer] = useState<number | null>(null);

  const handleAmountChange = (values: any) => {
    const { value } = values;
    setAmount(value || '');
    setValue('amount', value || '');

    // 기존 타이머 취소
    if (debounceTimer) {
      clearTimeout(debounceTimer);
    }

    // 값이 비어있거나 0이면 결과 초기화
    if (!value || isNaN(Number(value)) || Number(value) <= 0) {
      setResult(null);
      return;
    }

    // 즉시 변환 실행 (현재 환율이 있으면)
    if (result?.rate) {
      const convertedAmount = Number(value) * result.rate;
      if (!isNaN(convertedAmount) && isFinite(convertedAmount)) {
        setResult({
          ...result,
          amount: Number(value),
          converted_amount: convertedAmount,
        });
      }
    } else {
      // 환율이 없으면 즉시 API 호출
      handleConvert(value);
    }

    // API 호출은 디바운싱 (500ms 후 실행) - 환율 업데이트용
    const timer = setTimeout(() => {
      if (value && !isNaN(Number(value)) && Number(value) > 0) {
        handleConvert(value);
      }
    }, 500);

    setDebounceTimer(timer);
  };

  // 컴포넌트 언마운트 시 타이머 정리
  useEffect(() => {
    return () => {
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
    };
  }, [debounceTimer]);

  const handleRefresh = async (e?: React.MouseEvent) => {
    // 이벤트 전파 방지 (부모의 handleSwapCurrencies 실행 막기)
    if (e) {
      e.stopPropagation();
    }

    if (!amount || isNaN(Number(amount)) || Number(amount) <= 0) {
      return;
    }

    setIsRefreshing(true);
    await handleConvert(amount);
    setIsRefreshing(false);
  };

  return (
    <>

      {/* 변환 입력 카드 */}
      <div className="bg-gray-800 border border-gray-700 rounded-lg p-6 mb-6 space-y-5">
        {/* Amount */}
        <div>
          <label htmlFor="amount" className="block text-sm font-medium text-gray-300 mb-2">
            Amount
          </label>
          <NumericFormat
            id="amount"
            value={amount}
            onValueChange={handleAmountChange}
            placeholder="1.00"
            allowNegative={false}
            decimalScale={2}
            thousandSeparator=","
            className="w-full px-4 py-4 text-lg font-semibold bg-gray-700 border border-gray-600 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:border-cyan-400 focus:bg-gray-600 transition-all duration-200"
            aria-label="변환할 금액 입력"
          />
        </div>

        {/* From & To with Swap Button */}
        <div className="grid grid-cols-1 sm:grid-cols-[1fr_auto_1fr] gap-4 items-center">
          {/* From */}
          <div>
            <CurrencySelector
              value={fromCurrency}
              onChange={(newCurrency) => {
                setFromCurrency(newCurrency);
                if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
                  handleConvert(amount);
                }
              }}
              label="From"
            />
          </div>

          {/* Swap Button */}
          <div className="flex justify-center sm:mt-6">
            <motion.button
              onClick={handleSwapCurrencies}
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9, rotate: 180 }}
              transition={{ duration: 0.2 }}
              className="p-3 bg-gray-700 hover:bg-gray-600 rounded-full transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-red-500"
              aria-label="통화 교환"
            >
              <ArrowLeftRight className="h-5 w-5 text-gray-300" />
            </motion.button>
          </div>

          {/* To */}
          <div>
            <CurrencySelector
              value={toCurrency}
              onChange={(newCurrency) => {
                setToCurrency(newCurrency);
                if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
                  handleConvert(amount);
                }
              }}
              label="To"
            />
          </div>
        </div>
      </div>

      {/* 변환 결과 카드 */}
      <div
        className="bg-gray-800 border border-gray-700 rounded-lg p-6 cursor-pointer hover:border-gray-600 transition-colors duration-200"
        onClick={handleSwapCurrencies}
        role="button"
        tabIndex={0}
        aria-label="결과를 클릭하여 통화 교환"
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleSwapCurrencies();
          }
        }}
      >
        <AnimatePresence mode="wait">
          {(result || (!amount || Number(amount) === 0)) && !error && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3, ease: "easeOut" }}
            >
              <p className="text-sm text-gray-400 mb-2 text-center">
                {amount && Number(amount) > 0 && result ? formatNumber(result.amount) : '0'} {fromCurrency} =
              </p>
              <div className="flex items-center justify-center gap-3 mb-3">
                <motion.div
                  className="text-4xl font-bold text-white text-center"
                  initial={{ scale: 0.95 }}
                  animate={{ scale: 1 }}
                  transition={{ duration: 0.2, delay: 0.1 }}
                >
                  {amount && Number(amount) > 0 && result?.converted_amount && !isNaN(result.converted_amount) && isFinite(result.converted_amount) ? formatNumber(result.converted_amount) : '0'} <span className="text-red-400">{toCurrency}</span>
                </motion.div>
                <motion.button
                  onClick={handleRefresh}
                  disabled={isRefreshing || isLoading}
                  whileHover={{ scale: isRefreshing ? 1 : 1.1 }}
                  whileTap={{ scale: isRefreshing ? 1 : 0.9 }}
                  className={`p-2 rounded-full transition-colors duration-200 ${
                    isRefreshing || isLoading
                      ? 'text-gray-500 cursor-not-allowed'
                      : 'text-green-400 hover:text-green-300 hover:bg-gray-700'
                  }`}
                  aria-label="최신 환율 조회"
                >
                  <RefreshCw className={`h-5 w-5 ${isRefreshing ? 'animate-spin' : ''}`} />
                </motion.button>
              </div>
              <div className="text-center space-y-1">
                <div className="flex items-center justify-center gap-2">
                  <p className="text-sm text-gray-400">
                    1 {fromCurrency} = {result?.rate && !isNaN(result.rate) && isFinite(result.rate) ? formatNumber(result.rate) : '0'} {toCurrency}
                  </p>
                  {previousRate && result?.rate && previousRate !== result.rate && (
                    <div className={`flex items-center gap-1 text-xs font-medium ${result.rate > previousRate ? 'text-green-400' : 'text-red-400'}`}>
                      {result.rate > previousRate ? (
                        <TrendingUp className="h-3 w-3" />
                      ) : (
                        <TrendingDown className="h-3 w-3" />
                      )}
                      <span>{Math.abs(((result.rate - previousRate) / previousRate) * 100).toFixed(2)}%</span>
                    </div>
                  )}
                </div>
                {result && (
                  <p className="text-xs text-gray-500">
                    마지막 업데이트: {new Date(result.timestamp).toLocaleString('ko-KR')}
                  </p>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {error && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="p-5 bg-red-900/30 border-2 border-red-500 rounded-lg text-center space-y-3"
          >
            <p className="text-sm font-medium text-red-400">{error}</p>
            <button
              onClick={() => {
                setError('');
                handleConvert();
              }}
              className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-medium rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              다시 시도
            </button>
          </motion.div>
        )}

        {isLoading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center space-y-4"
          >
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
              className="inline-block w-8 h-8 border-4 border-gray-600 border-t-red-500 rounded-full"
            />
            <motion.p
              animate={{ opacity: [1, 0.5, 1] }}
              transition={{ duration: 1.5, repeat: Infinity }}
              className="text-sm text-gray-400"
            >
              환율 계산 중...
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
