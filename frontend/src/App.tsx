import { useState } from 'react';
import { Toaster } from 'react-hot-toast';
import Header from './components/Header';
import ConvertPage from './pages/ConvertPage';

function App() {
  const [activeTab, setActiveTab] = useState<string>('convert');

  return (
    <div className="min-h-screen bg-gray-900">
      <Header />
      <main>
        <ConvertPage activeTab={activeTab} onTabChange={setActiveTab} />
      </main>
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3000,
          style: {
            background: '#363636',
            color: '#fff',
            borderRadius: '8px',
          },
          success: {
            style: {
              background: '#10B981',
            },
          },
          error: {
            style: {
              background: '#EF4444',
            },
          },
        }}
      />
    </div>
  );
}

export default App;