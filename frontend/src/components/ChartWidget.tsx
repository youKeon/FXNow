import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowLeftRight, CalendarDays } from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceDot,
} from 'recharts';
import CurrencySelector from './CurrencySelector';
import DatePicker from './DatePicker';
import { api } from '../services/api';
import {
  defaultCurrencies,
  formatNumberFixed,
  formatDateTimeKST,
  formatDateKST,
} from '../utils/currencies';
import type { ExchangeRateChartResponse } from '../types';

interface ChartPoint {
  date: string;
  time?: string | null;
  rate: number;
  dayChange: number;
  axisLabel: string;
  tooltipLabel: string;
  iso: string;
}

const supportedFromCodes = new Set(['USD', 'EUR', 'JPY', 'CNY', 'GBP']);
const targetCurrencyCode = 'KRW';

const periodOptions = [
  { value: '1d', label: '1일', request: '1d' },
  { value: '1w', label: '1주', request: '1w' },
  { value: '1m', label: '1개월', request: '1m' },
  { value: '3m', label: '3개월', request: '3m' },
  { value: '1y', label: '1년', request: '1y' },
] as const;

const periodRequestMap = periodOptions.reduce<Record<string, string>>((acc, option) => {
  acc[option.value] = option.request;
  return acc;
}, { custom: '1y' });

const ChartWidget: React.FC = () => {
  const [fromCurrency, setFromCurrency] = useState<string>('USD');
  const [toCurrency, setToCurrency] = useState<string>(targetCurrencyCode);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('1w');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [rawChartData, setRawChartData] = useState<ChartPoint[]>([]);
  const [chartData, setChartData] = useState<ChartPoint[]>([]);
  const [hoverPoint, setHoverPoint] = useState<ChartPoint | null>(null);
  const [highRecord, setHighRecord] = useState<ChartPoint | null>(null);
  const [lowRecord, setLowRecord] = useState<ChartPoint | null>(null);
  const [statistics, setStatistics] = useState<{ high: number; average: number; low: number } | null>(null);
  const [chartMeta, setChartMeta] = useState<ExchangeRateChartResponse | null>(null);
  const [isRangePickerOpen, setIsRangePickerOpen] = useState(false);
  const [customRange, setCustomRange] = useState<{ start: string; end: string }>({ start: '', end: '' });

  const fromOptions = useMemo(
      () => defaultCurrencies.filter((currency) => supportedFromCodes.has(currency.code)),
      [],
  );

  const toOptions = useMemo(
      () => defaultCurrencies.filter((currency) => currency.code === targetCurrencyCode),
      [],
  );

  const parsePointDate = useCallback((point: ChartPoint): Date => {
    return new Date(point.iso);
  }, []);

  const applyFilters = useCallback(
      (data: ChartPoint[], periodValue: string): ChartPoint[] => {
        let filtered = data;

        if (periodValue === 'ytd') {
          const startOfYear = new Date(new Date().getFullYear(), 0, 1);
          filtered = filtered.filter((point) => parsePointDate(point) >= startOfYear);
        }

        if (customRange.start) {
          const startDate = new Date(`${customRange.start}T00:00:00+09:00`);
          filtered = filtered.filter((point) => parsePointDate(point) >= startDate);
        }

        if (customRange.end) {
          const endDate = new Date(`${customRange.end}T23:59:59+09:00`);
          filtered = filtered.filter((point) => parsePointDate(point) <= endDate);
        }

        return filtered;
      },
      [customRange.end, customRange.start, parsePointDate],
  );

  const computeStatistics = useCallback((data: ChartPoint[]) => {
    if (!data.length) {
      return {
        stats: null,
        highPoint: null,
        lowPoint: null,
      };
    }

    let highPoint = data[0];
    let lowPoint = data[0];
    let sum = 0;

    data.forEach((point) => {
      if (point.rate > highPoint.rate) {
        highPoint = point;
      }
      if (point.rate < lowPoint.rate) {
        lowPoint = point;
      }
      sum += point.rate;
    });

    return {
      stats: {
        high: highPoint.rate,
        low: lowPoint.rate,
        average: Number((sum / data.length).toFixed(2)),
      },
      highPoint,
      lowPoint,
    };
  }, []);

  const loadChartData = useCallback(async () => {
    const requestPeriod = periodRequestMap[selectedPeriod] ?? '1m';
    setIsLoading(true);
    setError(null);

    try {
      const response = await api.getExchangeHistory(fromCurrency, requestPeriod);

      const normalized: ExchangeRateChartResponse = {
        ...response,
        currentRate: Number(response.currentRate),
        change: Number(response.change),
        changePercent: Number(response.changePercent),
        chartData: response.chartData.map((point) => ({
          date: point.date,
          time: point.time,
          rate: Number(point.rate),
          dayChange: Number(point.dayChange),
        })),
        statistics: {
          high: Number(response.statistics.high),
          low: Number(response.statistics.low),
          average: Number(response.statistics.average),
        },
      };

      setChartMeta(normalized);

      const transformed: ChartPoint[] = normalized.chartData.map((point) => {
        const iso = point.time
            ? `${point.date}T${point.time}:00+09:00`
            : `${point.date}T00:00:00+09:00`;

        const axisLabel = selectedPeriod === '1d'
            ? new Date(iso).toLocaleTimeString('ko-KR', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: false,
              timeZone: 'Asia/Seoul',
            })
            : new Date(iso).toLocaleDateString('ko-KR', {
              month: 'short',
              day: 'numeric',
              timeZone: 'Asia/Seoul',
            });

        const tooltipLabel = formatDateTimeKST(iso);

        return {
          ...point,
          iso,
          axisLabel,
          tooltipLabel,
        };
      });

      setRawChartData(transformed);
      const filtered = applyFilters(transformed, selectedPeriod);
      const { stats, highPoint, lowPoint } = computeStatistics(filtered);

      setChartData(filtered);
      setStatistics(stats);
      setHighRecord(highPoint);
      setLowRecord(lowPoint);
    } catch (fetchError) {
      console.error('Failed to load chart data', fetchError);
      setError('차트 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
      setChartMeta(null);
      setRawChartData([]);
      setChartData([]);
      setStatistics(null);
      setHighRecord(null);
      setLowRecord(null);
    } finally {
      setIsLoading(false);
    }
  }, [applyFilters, computeStatistics, fromCurrency, selectedPeriod]);

  useEffect(() => {
    void loadChartData();
  }, [loadChartData]);

  useEffect(() => {
    const filtered = applyFilters(rawChartData, selectedPeriod);
    const { stats, highPoint, lowPoint } = computeStatistics(filtered);
    setChartData(filtered);
    setStatistics(stats);
    setHighRecord(highPoint);
    setLowRecord(lowPoint);
  }, [rawChartData, selectedPeriod, applyFilters, computeStatistics]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setHoverPoint(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const lastUpdatedLabel = chartMeta?.lastUpdated
      ? formatDateTimeKST(chartMeta.lastUpdated)
      : '';

  const targetCurrency = chartMeta?.targetCurrency ?? toCurrency;

  const displayPoint = hoverPoint ?? chartData[chartData.length - 1] ?? null;

  const handleMouseMove = useCallback((state: any) => {
    if (state?.isTooltipActive && state?.activePayload?.length) {
      setHoverPoint(state.activePayload[0].payload as ChartPoint);
    } else {
      setHoverPoint(null);
    }
  }, []);

  const handleMouseLeave = useCallback(() => {
    setHoverPoint(null);
  }, []);



  const handleRangeReset = () => {
    setCustomRange({ start: '', end: '' });
    setSelectedPeriod('1w');
    setIsRangePickerOpen(false);
  };

  const change = chartMeta ? chartMeta.change : null;
  const changePercent = chartMeta ? chartMeta.changePercent : null;
  const currentRate = chartMeta ? chartMeta.currentRate : null;
  const averageDiff = useMemo(() => {
    if (!statistics || currentRate === null) {
      return null;
    }
    const diff = statistics.average - currentRate;
    return {
      diff,
      diffPercent: currentRate !== 0 ? (diff / currentRate) * 100 : 0,
    };
  }, [statistics, currentRate]);

  return (
      <div className="space-y-6">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
          <div className="bg-gray-800 border border-gray-700 rounded-lg p-6 space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-end gap-4">
              <div className="flex-1">
                <CurrencySelector
                    value={fromCurrency}
                    onChange={setFromCurrency}
                    label="From"
                    currencies={fromOptions}
                />
              </div>
              <button
                  type="button"
                  aria-label="통화 전환"
                  title="현재 차트는 KRW 기준으로만 제공됩니다"
                  disabled
                  className="h-10 w-10 sm:h-12 sm:w-12 rounded-full bg-gray-700 border border-gray-600 text-gray-400 flex items-center justify-center cursor-not-allowed self-center"
              >
                <ArrowLeftRight className="h-5 w-5" />
              </button>
              <div className="flex-1">
                <CurrencySelector
                    value={toCurrency}
                    onChange={setToCurrency}
                    label="To"
                    currencies={toOptions}
                    disabled
                />
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm font-medium text-gray-300">조회 기간</span>
              </div>

              <div className="flex items-center justify-between gap-3">
                <div
                    className="inline-flex flex-wrap items-center gap-1 rounded-full bg-gray-700 p-1"
                    role="tablist"
                    aria-label="기간 선택"
                >
                  {periodOptions.map((period) => {
                    const isActive = selectedPeriod === period.value;
                    return (
                        <button
                            key={period.value}
                            type="button"
                            role="tab"
                            aria-selected={isActive}
                            onClick={() => {
                              setSelectedPeriod(period.value);
                              // Set date range based on period
                              if (period.value !== 'custom') {
                                const now = new Date();
                                let startDate = new Date();

                                switch (period.value) {
                                  case '1d':
                                    startDate.setDate(now.getDate() - 1);
                                    break;
                                  case '1w':
                                    startDate.setDate(now.getDate() - 7);
                                    break;
                                  case '1m':
                                    startDate.setMonth(now.getMonth() - 1);
                                    break;
                                  case '3m':
                                    startDate.setMonth(now.getMonth() - 3);
                                    break;
                                  case '1y':
                                    startDate.setFullYear(now.getFullYear() - 1);
                                    break;
                                  default:
                                    startDate = now;
                                }

                                setCustomRange({
                                  start: startDate.toISOString().split('T')[0],
                                  end: now.toISOString().split('T')[0]
                                });
                              }
                            }}
                            className={`px-3 py-1.5 text-xs font-medium rounded-full transition-colors duration-150 focus-visible:outline focus-visible:outline-2 focus-visible:outline-red-500 ${
                                isActive
                                    ? 'bg-red-500 text-white'
                                    : 'text-gray-300 hover:text-white'
                            }`}
                        >
                          {period.label}
                        </button>
                    );
                  })}
                </div>
              </div>

              <div className="flex flex-col sm:flex-row sm:flex-wrap items-start sm:items-center gap-4 rounded-lg border border-gray-600 bg-gray-700/60 p-4 h-20">
                <div className="flex items-center gap-2 w-full sm:w-auto h-full">
                  <label className="text-xs text-gray-300 min-w-[40px]">시작일</label>
                  <div className="flex-1 sm:min-w-[180px]">
                    <DatePicker
                        value={customRange.start}
                        onChange={(date) => {
                          setCustomRange((prev) => ({ ...prev, start: date }));
                          // Auto-apply when both dates are set
                          if (date && customRange.end) {
                            setSelectedPeriod('custom');
                          }
                        }}
                        placeholder=""
                    />
                  </div>
                </div>
                <div className="flex items-center gap-2 w-full sm:w-auto h-full">
                  <label className="text-xs text-gray-300 min-w-[40px]">종료일</label>
                  <div className="flex-1 sm:min-w-[180px]">
                    <DatePicker
                        value={customRange.end}
                        onChange={(date) => {
                          setCustomRange((prev) => ({ ...prev, end: date }));
                          // Auto-apply when both dates are set
                          if (date && customRange.start) {
                            setSelectedPeriod('custom');
                          }
                        }}
                        placeholder=""
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-gray-800 border border-gray-700 rounded-lg p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="text-sm text-gray-400">현재 환율</p>
                <div className="flex items-center gap-2">
                <span className="text-[34px] font-semibold text-white tabular-nums">
                  {currentRate !== null ? formatNumberFixed(currentRate) : '—'}
                </span>
                  <span className="text-sm text-gray-400">{targetCurrency}</span>
                  {change !== null && changePercent !== null ? (
                      <span className={`text-sm ml-2 ${change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                        {change >= 0 ? '+' : ''}{formatNumberFixed(change)} ({change >= 0 ? '+' : ''}{changePercent.toFixed(2)}%)
                      </span>
                  ) : null}
                </div>
              </div>
            </div>

            {/* 기간 통계 */}
            {statistics && highRecord && lowRecord ? (
                <div className="pt-4 border-t border-gray-700 sm:h-20">
                  <div className="grid gap-4 sm:grid-cols-3 text-sm text-gray-300 h-full">
                    <div className="flex flex-col justify-center space-y-1">
                      <p className="text-xs text-gray-500">최고</p>
                      <p className="text-lg font-semibold text-white tabular-nums">
                        {formatNumberFixed(statistics.high)}
                        <span className="ml-1 text-xs text-gray-400">{targetCurrency}</span>
                      </p>
                      <p className="text-xs text-gray-500">{formatDateKST(highRecord.iso)}</p>
                    </div>
                    <div className="flex flex-col justify-center space-y-1">
                      <p className="text-xs text-gray-500">평균</p>
                      <p className="text-lg font-semibold text-white tabular-nums">
                        {formatNumberFixed(statistics.average)}
                        <span className="ml-1 text-xs text-gray-400">{targetCurrency}</span>
                      </p>
                      {averageDiff && (
                          <p className={`text-xs ${averageDiff.diff >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                            {averageDiff.diff >= 0 ? '+' : ''}{formatNumberFixed(averageDiff.diff)} ({averageDiff.diff >= 0 ? '+' : ''}{averageDiff.diffPercent.toFixed(2)}%)
                          </p>
                      )}
                    </div>
                    <div className="flex flex-col justify-center space-y-1">
                      <p className="text-xs text-gray-500">최저</p>
                      <p className="text-lg font-semibold text-white tabular-nums">
                        {formatNumberFixed(statistics.low)}
                        <span className="ml-1 text-xs text-gray-400">{targetCurrency}</span>
                      </p>
                      <p className="text-xs text-gray-500">{formatDateKST(lowRecord.iso)}</p>
                    </div>
                  </div>
                </div>
            ) : null}
          </div>
        </div>

        <div className="bg-gray-900 rounded-lg border border-gray-700 p-6 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="space-y-1">
              <p className="text-sm text-gray-400">
                {fromCurrency} → {targetCurrency}
              </p>
              <p className="text-xs text-gray-500">
                마지막 업데이트 {lastUpdatedLabel}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3 text-right">
              {displayPoint && (
                  <div>
                    <p className="text-sm font-semibold text-white tabular-nums">
                      {formatNumberFixed(displayPoint.rate)} {targetCurrency}
                    </p>
                    <p className="text-xs text-gray-500">
                      {formatDateTimeKST(displayPoint.iso)}
                    </p>
                  </div>
              )}
            </div>
          </div>

          <div className="h-72">
            {isLoading ? (
                <div className="flex items-center justify-center h-full text-gray-400">
                  차트 데이터를 불러오는 중...
                </div>
            ) : error ? (
                <div className="flex flex-col items-center justify-center h-full space-y-2 text-center">
                  <p className="text-sm text-red-400">{error}</p>
                  <button
                      onClick={() => void loadChartData()}
                      className="px-3 py-2 text-xs font-medium bg-red-500 text-white rounded hover:bg-red-400"
                  >
                    다시 시도
                  </button>
                </div>
            ) : chartData.length === 0 ? (
                <div className="flex items-center justify-center h-full text-gray-400">
                  차트 데이터가 없습니다.
                </div>
            ) : (
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart
                        data={chartData}
                        margin={{ top: 10, right: 24, left: 0, bottom: 20 }}
                        onMouseMove={handleMouseMove}
                        onMouseLeave={handleMouseLeave}
                    >
                      <CartesianGrid stroke="rgba(148, 163, 184, 0.15)" />
                      <XAxis
                          dataKey="axisLabel"
                          tick={{ fontSize: 11, fill: '#9CA3AF' }}
                          axisLine={false}
                          tickLine={false}
                      />
                      <YAxis
                          tick={{ fontSize: 11, fill: '#9CA3AF' }}
                          axisLine={false}
                          tickLine={false}
                          domain={['dataMin - 5', 'dataMax + 5']}
                          tickFormatter={(value) => formatNumberFixed(value as number)}
                      />
                      <Tooltip
                          cursor={{ stroke: 'rgba(148, 163, 184, 0.45)', strokeWidth: 1 }}
                          content={({ active, payload }) => {
                            if (active && payload && payload.length) {
                              const point = payload[0];
                              const datum = point.payload as ChartPoint;
                              return (
                                  <div className="bg-gray-800 border border-gray-600 rounded-lg p-3 shadow-lg space-y-1">
                                    <p className="text-xs font-medium text-gray-200">
                                      {formatDateTimeKST(datum.iso)}
                                    </p>
                                    <p className="text-sm text-white tabular-nums">
                                      {formatNumberFixed(point.value as number)} {targetCurrency}
                                    </p>
                                    <p className={`text-[11px] ${datum.dayChange >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                                      {datum.dayChange >= 0 ? '+' : ''}{datum.dayChange.toFixed(2)}%
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
                      {highRecord && (
                          <ReferenceDot
                              x={highRecord.axisLabel}
                              y={highRecord.rate}
                              r={6}
                              fill="#22d3ee"
                              stroke="#0f172a"
                              strokeWidth={2}
                          />
                      )}
                      {lowRecord && (
                          <ReferenceDot
                              x={lowRecord.axisLabel}
                              y={lowRecord.rate}
                              r={6}
                              fill="#a855f7"
                              stroke="#0f172a"
                              strokeWidth={2}
                          />
                      )}
                    </LineChart>
                  </ResponsiveContainer>

                </div>
            )}
          </div>
        </div>

      </div>
  );
};

export default ChartWidget;
