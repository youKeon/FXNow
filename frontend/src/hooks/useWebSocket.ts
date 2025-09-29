import { useEffect, useRef, useState } from 'react';
import { webSocketService, ExchangeRateUpdate } from '../services/websocket';

export const useWebSocket = () => {
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const connectWebSocket = async () => {
      try {
        await webSocketService.connect();
        setIsConnected(true);
      } catch (error) {
        console.error('Failed to connect to WebSocket:', error);
        setIsConnected(false);
      }
    };

    connectWebSocket();

    return () => {
      webSocketService.disconnect();
      setIsConnected(false);
    };
  }, []);

  return { isConnected };
};

export const useExchangeRateUpdates = (callback: (update: ExchangeRateUpdate) => void) => {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    const unsubscribe = webSocketService.subscribeToExchangeRates((update) => {
      callbackRef.current(update);
    });

    return unsubscribe;
  }, []);
};

export const useSpecificExchangeRate = (
  from: string,
  to: string,
  callback: (update: ExchangeRateUpdate) => void
) => {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    const unsubscribe = webSocketService.subscribeToSpecificRate(from, to, (update) => {
      callbackRef.current(update);
    });

    return unsubscribe;
  }, [from, to]);
};