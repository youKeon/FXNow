import React, { useState } from 'react';
import Header from './components/Header';
import ConvertPage from './pages/ConvertPage';
import ChartsPage from './pages/ChartsPage';
import AlertsPage from './pages/AlertsPage';

function App() {
  const [activeTab, setActiveTab] = useState<string>('convert');

  const renderActiveTab = () => {
    switch (activeTab) {
      case 'convert':
        return <ConvertPage activeTab={activeTab} onTabChange={setActiveTab} />;
      case 'charts':
        return <ChartsPage activeTab={activeTab} onTabChange={setActiveTab} />;
      case 'alerts':
        return <AlertsPage activeTab={activeTab} onTabChange={setActiveTab} />;
      default:
        return <ConvertPage activeTab={activeTab} onTabChange={setActiveTab} />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main>
        {renderActiveTab()}
      </main>
    </div>
  );
}

export default App;