import { useState } from 'react';
import { Link } from 'react-router-dom';
import { analysisApi, ErrorStackAnalysisResponse, repositoryApi, Repository } from '../services/api';
import { useEffect } from 'react';

const ErrorStackAnalysis: React.FC = () => {
  const [errorStack, setErrorStack] = useState('');
  const [selectedRepoId, setSelectedRepoId] = useState<string>('');
  const [repositories, setRepositories] = useState<Record<string, Repository>>({});
  const [analysis, setAnalysis] = useState<ErrorStackAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingRepos, setLoadingRepos] = useState(true);

  useEffect(() => {
    loadRepositories();
  }, []);

  const loadRepositories = async () => {
    try {
      setLoadingRepos(true);
      const response = await repositoryApi.getAll();
      setRepositories(response.data);
    } catch (error) {
      console.error('Failed to load repositories:', error);
    } finally {
      setLoadingRepos(false);
    }
  };

  const handleAnalyze = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!errorStack.trim()) return;

    try {
      setLoading(true);
      const response = await analysisApi.analyzeErrorStack({
        errorStack,
        repositoryId: selectedRepoId || undefined,
      });
      setAnalysis(response.data);
    } catch (error) {
      console.error('Failed to analyze error stack:', error);
      alert('分析错误堆栈失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-800">🐛 错误堆栈分析</h1>
        <Link to="/" className="text-blue-600 hover:underline">
          ← 返回
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4 text-gray-800">输入错误堆栈</h2>
        <form onSubmit={handleAnalyze} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              选择关联仓库（可选）
            </label>
            <select
              value={selectedRepoId}
              onChange={(e) => setSelectedRepoId(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={loadingRepos}
            >
              <option value="">不关联仓库</option>
              {Object.values(repositories).map((repo) => (
                <option key={repo.id} value={repo.id}>
                  {repo.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              错误堆栈
            </label>
            <textarea
              value={errorStack}
              onChange={(e) => setErrorStack(e.target.value)}
              placeholder="粘贴错误堆栈信息..."
              rows={10}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono text-sm"
            />
          </div>
          <button
            type="submit"
            disabled={!errorStack.trim() || loading}
            className="w-full bg-red-600 text-white py-2 px-4 rounded-lg hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? '分析中...' : '分析错误堆栈'}
          </button>
        </form>
      </div>

      {analysis && (
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">分析结果</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <h3 className="text-lg font-medium mb-2 text-gray-800">错误类型</h3>
                <span className="bg-red-100 text-red-800 px-3 py-1 rounded-full text-sm">
                  {analysis.errorType}
                </span>
              </div>
              <div>
                <h3 className="text-lg font-medium mb-2 text-gray-800">根本原因</h3>
                <p className="text-gray-700">{analysis.rootCause}</p>
              </div>
            </div>

            <div className="mb-6">
              <h3 className="text-lg font-medium mb-2 text-gray-800">摘要</h3>
              <p className="text-gray-700">{analysis.summary}</p>
            </div>
          </div>

          {analysis.suspectedLocations.length > 0 && (
            <div className="bg-white rounded-lg shadow-md p-6">
              <h3 className="text-lg font-medium mb-4 text-gray-800">可疑位置</h3>
              <div className="space-y-4">
                {analysis.suspectedLocations.map((location, index) => (
                  <div key={index} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium text-gray-800">
                        {location.className}.{location.methodName}
                      </span>
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        location.confidence > 0.8 ? 'bg-green-100 text-green-800' :
                        location.confidence > 0.5 ? 'bg-yellow-100 text-yellow-800' :
                        'bg-gray-100 text-gray-800'
                      }`}>
                        置信度: {(location.confidence * 100).toFixed(0)}%
                      </span>
                    </div>
                    <p className="text-sm text-gray-600 mb-1">
                      {location.filePath}:{location.lineNumber}
                    </p>
                    <p className="text-gray-700">{location.description}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {analysis.possibleFixes.length > 0 && (
            <div className="bg-white rounded-lg shadow-md p-6">
              <h3 className="text-lg font-medium mb-4 text-gray-800">可能的修复方案</h3>
              <ul className="space-y-2">
                {analysis.possibleFixes.map((fix, index) => (
                  <li key={index} className="flex items-start">
                    <span className="text-green-600 mr-2">✅</span>
                    <span className="text-gray-700">{fix}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {analysis.relatedCode.length > 0 && (
            <div className="bg-white rounded-lg shadow-md p-6">
              <h3 className="text-lg font-medium mb-4 text-gray-800">相关代码</h3>
              <div className="space-y-4">
                {analysis.relatedCode.map((code, index) => (
                  <div key={index}>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium text-gray-800 font-mono text-sm">
                        {code.filePath}
                      </span>
                      <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs">
                        {code.relevance}
                      </span>
                    </div>
                    <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto text-xs max-h-48 overflow-y-auto">
                      <code>{code.codeSnippet}</code>
                    </pre>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ErrorStackAnalysis;
