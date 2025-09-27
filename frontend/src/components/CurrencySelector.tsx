import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Search } from 'lucide-react';
import { defaultCurrencies } from '../utils/currencies';
import type { Currency } from '../types';

interface CurrencySelectorProps {
  value: string;
  onChange: (currency: string) => void;
  label: string;
  className?: string;
}

const CurrencySelector: React.FC<CurrencySelectorProps> = ({ value, onChange, label, className = '' }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);

  const selectedCurrency = defaultCurrencies.find(c => c.code === value);
  const filteredCurrencies = defaultCurrencies.filter(currency =>
    currency.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
    currency.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setSearchQuery('');
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (currency: Currency) => {
    onChange(currency.code);
    setIsOpen(false);
    setSearchQuery('');
  };

  return (
    <div className={`relative ${className}`} ref={dropdownRef}>
      <label className="block text-sm font-medium text-gray-300 mb-2">{label}</label>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full bg-gray-700 border border-gray-600 rounded-lg px-4 py-4 text-left hover:border-gray-500 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all duration-200"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <span className="text-xl">{selectedCurrency?.flag}</span>
            <div>
              <span className="block text-sm font-medium text-white">
                {selectedCurrency?.code}
              </span>
              <span className="block text-xs text-gray-300">
                {selectedCurrency?.name}
              </span>
            </div>
          </div>
          <ChevronDown className={`h-4 w-4 text-gray-400 transition-transform duration-200 ${
            isOpen ? 'transform rotate-180' : ''
          }`} />
        </div>
      </button>

      {isOpen && (
        <div className="absolute z-20 mt-1 w-full bg-gray-800 border border-gray-600 rounded-lg shadow-lg max-h-80 overflow-hidden">
          <div className="p-3 border-b border-gray-600">
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
              <input
                type="text"
                placeholder="통화 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-gray-700 border border-gray-600 text-white rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>
          </div>
          <div className="max-h-64 overflow-y-auto">
            {filteredCurrencies.map((currency) => (
              <button
                key={currency.code}
                onClick={() => handleSelect(currency)}
                className="w-full px-4 py-3 text-left hover:bg-gray-700 focus:outline-none focus:bg-gray-700 border-b border-gray-600 last:border-b-0 transition-colors duration-150"
              >
                <div className="flex items-center space-x-3">
                  <span className="text-lg">{currency.flag}</span>
                  <div>
                    <span className="block text-sm font-medium text-white">
                      {currency.code}
                    </span>
                    <span className="block text-xs text-gray-300">
                      {currency.name}
                    </span>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default CurrencySelector;