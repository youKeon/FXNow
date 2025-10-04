import React from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, TrendingUp, TrendingDown } from 'lucide-react';
import { formatNumber } from '../utils/currencies';
import { ARIA_LABELS } from '../constants/currency';
import type { ConversionResult as ConversionResultType } from '../types';

interface ConversionResultProps {
  result: ConversionResultType | null;
  amount: string;
  fromCurrency: string;
  toCurrency: string;
  previousRate: number | null;
  onRefresh: (e: React.MouseEvent) => void;
  isRefreshing: boolean;
  isLoading: boolean;
  isDemoMode: boolean;
}

const ConversionResult: React.FC<ConversionResultProps> = ({
  result,
  amount,
  fromCurrency,
  toCurrency,
  previousRate,
  onRefresh,
  isRefreshing,
  isLoading,
  isDemoMode,
}) => {
  return (
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
          {amount && Number(amount) > 0 && result?.convertedAmount && !isNaN(result.convertedAmount) && isFinite(result.convertedAmount) ? formatNumber(result.convertedAmount) : '0'} <span className="text-red-400">{toCurrency}</span>
        </motion.div>
        <motion.button
          onClick={onRefresh}
          disabled={isRefreshing || isLoading}
          whileHover={{ scale: isRefreshing ? 1 : 1.1 }}
          whileTap={{ scale: isRefreshing ? 1 : 0.9 }}
          animate={isRefreshing ? { rotate: 360 } : { rotate: 0 }}
          transition={isRefreshing ? { duration: 0.6, repeat: Infinity, ease: "linear" } : { duration: 0.15 }}
          className={`p-2 rounded-lg transition-colors duration-200 ${
            isRefreshing || isLoading
              ? 'text-gray-500 cursor-not-allowed'
              : 'text-red-400 hover:bg-red-500/10'
          }`}
          aria-label={ARIA_LABELS.REFRESH_RATE}
        >
          <RefreshCw className="h-5 w-5" />
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
        {result && !isRefreshing && (
          <div className="space-y-1">
            <p className="text-xs text-gray-500">
              마지막 업데이트: {new Date(result.timestamp).toLocaleString('ko-KR')}
            </p>
            {isDemoMode && (
              <p className="text-xs text-yellow-400 flex items-center justify-center gap-1">
                ⚠️ 오프라인 모드 (데모 데이터 사용 중)
              </p>
            )}
          </div>
        )}
      </div>
    </motion.div>
  );
};

export default ConversionResult;
