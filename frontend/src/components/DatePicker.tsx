import React, { useState, useRef, useEffect } from 'react';
import { ChevronLeft, ChevronRight, CalendarDays } from 'lucide-react';

interface DatePickerProps {
  value: string;
  onChange: (date: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

const DatePicker: React.FC<DatePickerProps> = ({
  value,
  onChange,
  placeholder = '날짜',
  disabled = false,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [currentDate, setCurrentDate] = useState(() => {
    const date = value ? new Date(value) : new Date();
    return new Date(date.getFullYear(), date.getMonth(), 1);
  });

  const containerRef = useRef<HTMLDivElement>(null);

  const selectedDate = value ? new Date(value) : null;
  const today = new Date();

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getDaysInMonth = (date: Date): Date[] => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    const days: Date[] = [];

    for (let i = startingDayOfWeek - 1; i >= 0; i--) {
      const prevDay = new Date(year, month, -i);
      days.push(prevDay);
    }

    for (let day = 1; day <= daysInMonth; day++) {
      days.push(new Date(year, month, day));
    }

    const remainingDays = 42 - days.length;
    for (let day = 1; day <= remainingDays; day++) {
      days.push(new Date(year, month + 1, day));
    }

    return days;
  };

  const formatDate = (date: Date): string => date.toISOString().split('T')[0];

  const formatDisplayDate = (dateString: string): string => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const handleDateClick = (date: Date) => {
    onChange(formatDate(date));
    setIsOpen(false);
  };

  const handlePrevMonth = () => {
    setCurrentDate((prev) => new Date(prev.getFullYear(), prev.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentDate((prev) => new Date(prev.getFullYear(), prev.getMonth() + 1, 1));
  };

  const isToday = (date: Date): boolean => date.toDateString() === today.toDateString();

  const isSelected = (date: Date): boolean => {
    if (!selectedDate) return false;
    return date.toDateString() === selectedDate.toDateString();
  };

  const isCurrentMonth = (date: Date): boolean => date.getMonth() === currentDate.getMonth();

  const days = getDaysInMonth(currentDate);
  const monthYear = currentDate.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
  });

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={`flex items-center justify-between w-full px-3 py-2 text-sm rounded-md border h-10 min-w-[200px] ${
          disabled
            ? 'bg-gray-700 border-gray-600 text-gray-500 cursor-not-allowed'
            : 'bg-gray-800 border-gray-600 text-gray-100 hover:border-red-400 focus:border-red-400 focus:outline-none focus:ring-1 focus:ring-red-500'
        }`}
      >
        <span className={value ? 'text-gray-100' : 'text-gray-400'}>
          {value ? formatDisplayDate(value) : placeholder}
        </span>
        <CalendarDays className="h-4 w-4 text-gray-400" />
      </button>

      {isOpen && (
        <div className="absolute z-50 bg-gray-800 border border-gray-600 rounded-lg shadow-2xl p-4 w-80 mt-1 left-0"
          style={{ minWidth: '320px' }}
        >
          <div className="flex items-center justify-between mb-4">
            <button
              type="button"
              onClick={handlePrevMonth}
              className="p-1 rounded-md hover:bg-gray-700 text-gray-300 hover:text-white"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <h3 className="font-medium text-gray-100">{monthYear}</h3>
            <button
              type="button"
              onClick={handleNextMonth}
              className="p-1 rounded-md hover:bg-gray-700 text-gray-300 hover:text-white"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-1 mb-2">
            {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
              <div key={day} className="h-8 flex items-center justify-center text-xs font-medium text-gray-400">
                {day}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1">
            {days.map((date) => {
              const dateKey = formatDate(date);
              return (
                <button
                  key={dateKey}
                  type="button"
                  onClick={() => handleDateClick(date)}
                  className={`h-8 w-8 text-sm rounded-md transition-colors duration-150 ${
                    !isCurrentMonth(date)
                      ? 'text-gray-600 hover:text-gray-400 hover:bg-gray-700'
                      : isSelected(date)
                      ? 'bg-red-500 text-white'
                      : isToday(date)
                        ? 'bg-gray-700 text-red-400 hover:bg-gray-600'
                        : 'text-gray-300 hover:bg-gray-700 hover:text-white'
                  }`}
                >
                  {date.getDate()}
                </button>
              );
            })}
          </div>

          <div className="mt-4 pt-3 border-t border-gray-700">
            <button
              type="button"
              onClick={() => {
                onChange(formatDate(today));
                setIsOpen(false);
              }}
              className="w-full py-2 text-sm text-red-400 hover:text-red-300 hover:bg-gray-700 rounded-md transition-colors duration-150"
            >
              오늘
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default DatePicker;
