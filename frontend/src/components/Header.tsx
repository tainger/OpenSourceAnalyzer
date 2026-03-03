import { Link } from 'react-router-dom';

const Header = () => {
  return (
    <header className="bg-gradient-to-r from-blue-600 to-purple-600 text-white shadow-lg">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <Link to="/" className="flex items-center space-x-3">
            <div className="text-3xl">🤖</div>
            <div>
              <h1 className="text-2xl font-bold">AI Source Code Analyzer</h1>
              <p className="text-blue-100 text-sm">智能源码分析工具</p>
            </div>
          </Link>
          <nav className="flex space-x-4">
            <Link 
              to="/" 
              className="px-4 py-2 rounded-lg hover:bg-white/20 transition-colors"
            >
              仓库管理
            </Link>
            <Link 
              to="/error-analysis" 
              className="px-4 py-2 rounded-lg hover:bg-white/20 transition-colors"
            >
              错误分析
            </Link>
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Header;
