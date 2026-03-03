import { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/Header';
import RepositoryList from './pages/RepositoryList';
import ArchitectureAnalysis from './pages/ArchitectureAnalysis';
import CodeWalkthrough from './pages/CodeWalkthrough';
import ErrorStackAnalysis from './pages/ErrorStackAnalysis';
import ChatDialog from './pages/ChatDialog';

function App() {
  const [selectedRepo, setSelectedRepo] = useState<string | null>(null);

  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Header />
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route 
              path="/" 
              element={
                <RepositoryList 
                  onSelectRepo={setSelectedRepo}
                  selectedRepo={selectedRepo}
                />
              } 
            />
            <Route 
              path="/architecture/:repoId" 
              element={<ArchitectureAnalysis />} 
            />
            <Route 
              path="/walkthrough/:repoId" 
              element={<CodeWalkthrough />} 
            />
            <Route 
              path="/error-analysis" 
              element={<ErrorStackAnalysis />} 
            />
            <Route 
              path="/chat/:repoId" 
              element={<ChatDialog />} 
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
