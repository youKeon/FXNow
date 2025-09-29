import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface ExchangeRateUpdate {
  from: string;
  to: string;
  rate: number;
  timestamp: string;
}

type ExchangeRateCallback = (update: ExchangeRateUpdate) => void;

class WebSocketService {
  private client: Client | null = null;
  private callbacks: Set<ExchangeRateCallback> = new Set();
  private isConnected = false;

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        debug: (str) => {
          console.log('STOMP Debug:', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.client.onConnect = () => {
        console.log('WebSocket connected');
        this.isConnected = true;

        // 모든 환율 업데이트 구독
        this.client?.subscribe('/topic/exchange-rates', (message) => {
          const update: ExchangeRateUpdate = JSON.parse(message.body);
          this.notifyCallbacks(update);
        });

        resolve();
      };

      this.client.onStompError = (frame) => {
        console.error('STOMP error:', frame);
        this.isConnected = false;
        reject(new Error('WebSocket connection failed'));
      };

      this.client.onWebSocketError = (error) => {
        console.error('WebSocket error:', error);
        this.isConnected = false;
        reject(error);
      };

      this.client.onDisconnect = () => {
        console.log('WebSocket disconnected');
        this.isConnected = false;
      };

      this.client.activate();
    });
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.isConnected = false;
    }
  }

  subscribeToExchangeRates(callback: ExchangeRateCallback): () => void {
    this.callbacks.add(callback);

    // 연결이 안되어 있다면 자동으로 연결
    if (!this.isConnected && !this.client?.active) {
      this.connect().catch(console.error);
    }

    // unsubscribe 함수 반환
    return () => {
      this.callbacks.delete(callback);
    };
  }

  subscribeToSpecificRate(from: string, to: string, callback: ExchangeRateCallback): () => void {
    // 서버에 해당 통화 쌍 구독 요청
    this.subscribeToRateOnServer(from, to);

    const filteredCallback: ExchangeRateCallback = (update) => {
      if (update.from === from && update.to === to) {
        callback(update);
      }
    };

    const unsubscribe = this.subscribeToExchangeRates(filteredCallback);

    // unsubscribe 함수를 확장하여 서버에도 구독 해제 요청
    return () => {
      this.unsubscribeFromRateOnServer(from, to);
      unsubscribe();
    };
  }

  private subscribeToRateOnServer(from: string, to: string): void {
    const trySubscribe = () => {
      if (this.client?.connected) {
        this.client.publish({
          destination: '/app/subscribe',
          body: JSON.stringify({ from, to })
        });
        console.log(`Subscribed to rate updates: ${from} -> ${to}`);
      } else {
        console.log(`WebSocket not connected, retrying subscription for ${from} -> ${to}`);
        // 연결되지 않았다면 1초 후 재시도
        setTimeout(trySubscribe, 1000);
      }
    };

    trySubscribe();
  }

  private unsubscribeFromRateOnServer(from: string, to: string): void {
    if (this.client?.connected) {
      this.client.publish({
        destination: '/app/unsubscribe',
        body: JSON.stringify({ from, to })
      });
      console.log(`Unsubscribed from rate updates: ${from} -> ${to}`);
    }
  }

  private notifyCallbacks(update: ExchangeRateUpdate): void {
    this.callbacks.forEach(callback => {
      try {
        callback(update);
      } catch (error) {
        console.error('Error in WebSocket callback:', error);
      }
    });
  }

  getConnectionStatus(): boolean {
    return this.isConnected;
  }
}

// 싱글톤 인스턴스
export const webSocketService = new WebSocketService();
export type { ExchangeRateUpdate };