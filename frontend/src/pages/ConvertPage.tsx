import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { NumericFormat, NumberFormatValues } from 'react-number-format';
import { ArrowLeftRight } from 'lucide-react';
import CurrencySelector from '../components/CurrencySelector';
import ChartWidget from '../components/ChartWidget';
import AlertWidget from '../components/AlertWidget';
import ConversionResult from '../components/ConversionResult';
import { api } from '../services/api';
import { CURRENCY_CONFIG, DEMO_RATES, ARIA_LABELS } from '../constants/currency';
import type { ConversionResult as ConversionResultType } from '../types';

interface ConvertPageProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

// API 응답 검증 유틸 함수
const isValidConversionResult = (result: ConversionResultType): boolean => {
  return !isNaN(result.convertedAmount) &&
         isFinite(result.convertedAmount) &&
         !isNaN(result.rate) &&
         isFinite(result.rate);
};

// 변환기 컴포넌트 분리
const CurrencyConverter: React.FC = () => {
  const [amount, setAmount] = useState<string>(CURRENCY_CONFIG.DEFAULT_AMOUNT);
  const [fromCurrency, setFromCurrency] = useState<string>(CURRENCY_CONFIG.DEFAULT_FROM);
  const [toCurrency, setToCurrency] = useState<string>(CURRENCY_CONFIG.DEFAULT_TO);
  const [result, setResult] = useState<ConversionResultType | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [isDemoMode, setIsDemoMode] = useState<boolean>(false);

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const resultRef = useRef<ConversionResultType | null>(null);

  // resultRef 동기화
  useEffect(() => {
    resultRef.current = result;
  }, [result]);

  const executeConversion = useCallback(async (
    convertAmount: string,
    sourceCurrency: string,
    targetCurrency: string
  ) => {
    if (!convertAmount || isNaN(Number(convertAmount)) || Number(convertAmount) <= 0) {
      setError('');
      setResult(null);
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const apiResponse = await api.convertCurrency(Number(convertAmount), sourceCurrency, targetCurrency);

      const conversionResult: ConversionResultType = {
        convertedAmount: apiResponse.convertedAmount,
        rate: apiResponse.rate,
        timestamp: apiResponse.timestamp,
        from: sourceCurrency,
        to: targetCurrency,
        amount: Number(convertAmount)
      };

      if (isValidConversionResult(conversionResult)) {
        setResult(conversionResult);
        setIsDemoMode(false);
      }
    } catch (err) {
      console.warn('API failed, using demo data:', err);
      setIsDemoMode(true);

      const rate = DEMO_RATES[sourceCurrency]?.[targetCurrency] || 1;
      const convertedAmount = Number(convertAmount) * rate;

      const demoResult: ConversionResultType = {
        convertedAmount,
        from: sourceCurrency,
        to: targetCurrency,
        amount: Number(convertAmount),
        rate,
        timestamp: new Date().toISOString()
      };

      if (isValidConversionResult(demoResult)) {
        setResult(demoResult);
      }
      setError('');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const handleConvert = useCallback(async (formAmount?: string) => {
    const currentAmount = formAmount || amount;
    await executeConversion(currentAmount, fromCurrency, toCurrency);
  }, [amount, fromCurrency, toCurrency, executeConversion]);

  const handleSwapCurrencies = useCallback(() => {
    const newFromCurrency = toCurrency;
    const newToCurrency = fromCurrency;

    setFromCurrency(newFromCurrency);
    setToCurrency(newToCurrency);

  }, [toCurrency, fromCurrency]);

  // Auto-convert when currency changes (amount changes are handled by debounce)
  useEffect(() => {
    if (!amount || isNaN(Number(amount)) || Number(amount) <= 0) {
      setResult(null);
      return;
    }

    const timerId = window.setTimeout(() => {
      executeConversion(amount, fromCurrency, toCurrency);
    }, 0);

    return () => window.clearTimeout(timerId);
    // amount intentionally omitted to avoid triggering on value edits (handled elsewhere)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fromCurrency, toCurrency, executeConversion]);

  const handleAmountChange = useCallback((values: NumberFormatValues) => {
    const { value } = values;
    setAmount(value || '');

    // 기존 타이머 취소
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    // 값이 비어있거나 0이면 결과 초기화
    if (!value || isNaN(Number(value)) || Number(value) <= 0) {
      setResult(null);
      return;
    }

    // 즉시 변환 실행 (현재 환율이 있으면)
    const currentResult = resultRef.current;
    if (currentResult?.rate) {
      const convertedAmount = Number(value) * currentResult.rate;
      if (!isNaN(convertedAmount) && isFinite(convertedAmount)) {
        setResult({
          ...currentResult,
          amount: Number(value),
          convertedAmount: convertedAmount,
        });
      }
    } else {
      // 환율이 없으면 즉시 API 호출
      handleConvert(value);
    }

    // API 호출은 디바운싱 - 환율 업데이트용
    debounceTimerRef.current = setTimeout(() => {
      if (value && !isNaN(Number(value)) && Number(value) > 0) {
        handleConvert(value);
      }
    }, CURRENCY_CONFIG.DEBOUNCE_MS);
  }, [handleConvert]);

  // 컴포넌트 언마운트 시 타이머 정리
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  const handleRefresh = useCallback(async (e: React.MouseEvent) => {
    // 이벤트 전파 방지 (부모의 handleSwapCurrencies 실행 막기)
    e.stopPropagation();

    if (!amount || isNaN(Number(amount)) || Number(amount) <= 0) {
      return;
    }

    setIsRefreshing(true);
    await handleConvert(amount);
    setIsRefreshing(false);
  }, [amount, handleConvert]);

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
            placeholder={CURRENCY_CONFIG.DEFAULT_AMOUNT}
            allowNegative={false}
            decimalScale={CURRENCY_CONFIG.DECIMAL_SCALE}
            thousandSeparator=","
            className="w-full px-4 py-4 text-lg font-semibold bg-gray-700 border border-gray-600 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:border-cyan-400 focus:bg-gray-600 transition-all duration-200"
            aria-label={ARIA_LABELS.AMOUNT_INPUT}
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
              aria-label={ARIA_LABELS.SWAP_CURRENCIES}
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
        aria-label={ARIA_LABELS.CLICK_TO_SWAP}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleSwapCurrencies();
          }
        }}
      >
        <AnimatePresence mode="wait">
          {(result || (!amount || Number(amount) === 0)) && !error && (
            <ConversionResult
              result={result}
              amount={amount}
              fromCurrency={fromCurrency}
              toCurrency={toCurrency}
              onRefresh={handleRefresh}
              isRefreshing={isRefreshing}
              isLoading={isLoading}
              isDemoMode={isDemoMode}
            />
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
