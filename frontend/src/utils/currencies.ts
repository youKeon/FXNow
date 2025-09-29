import type { Currency } from '../types';

export const defaultCurrencies: Currency[] = [
  { code: 'USD', name: 'US Dollar', symbol: '$', flag: '🇺🇸' },
  { code: 'EUR', name: 'Euro', symbol: '€', flag: '🇪🇺' },
  { code: 'KRW', name: 'South Korean Won', symbol: '₩', flag: '🇰🇷' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥', flag: '🇯🇵' },
  { code: 'CNY', name: 'Chinese Yuan', symbol: '¥', flag: '🇨🇳' },
  { code: 'GBP', name: 'British Pound', symbol: '£', flag: '🇬🇧' },
  { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$', flag: '🇨🇦' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$', flag: '🇦🇺' },
  { code: 'CHF', name: 'Swiss Franc', symbol: 'Fr', flag: '🇨🇭' },
  { code: 'SEK', name: 'Swedish Krona', symbol: 'kr', flag: '🇸🇪' },
];

export const formatCurrency = (amount: number, currency: string): string => {
  const currencyInfo = defaultCurrencies.find(c => c.code === currency);
  if (!currencyInfo) return `${amount} ${currency}`;

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(amount);
};

export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 6,
  }).format(num);
};

export const formatNumberFixed = (num: number, fractionDigits = 2): string => {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
    useGrouping: true,
  }).format(num);
};

export const formatDateTimeKST = (input: string | Date): string => {
  const utcDate = typeof input === 'string' ? new Date(input) : input;
  if (Number.isNaN(utcDate.getTime())) {
    return '';
  }

  const zoned = new Date(
    utcDate.toLocaleString('en-US', { timeZone: 'Asia/Seoul' }),
  );

  const pad = (value: number) => value.toString().padStart(2, '0');

  const year = zoned.getFullYear();
  const month = pad(zoned.getMonth() + 1);
  const day = pad(zoned.getDate());
  const hour = pad(zoned.getHours());
  const minute = pad(zoned.getMinutes());
  const second = pad(zoned.getSeconds());

  return `${year}-${month}-${day} ${hour}:${minute}:${second} KST`;
};

export const formatDateKST = (input: string | Date): string => {
  const utcDate = typeof input === 'string' ? new Date(input) : input;
  if (Number.isNaN(utcDate.getTime())) {
    return '';
  }

  const zoned = new Date(
    utcDate.toLocaleString('en-US', { timeZone: 'Asia/Seoul' }),
  );

  const pad = (value: number) => value.toString().padStart(2, '0');

  const year = zoned.getFullYear();
  const month = pad(zoned.getMonth() + 1);
  const day = pad(zoned.getDate());

  return `${year}-${month}-${day}`;
};
