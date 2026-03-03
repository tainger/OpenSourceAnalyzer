import { useState } from 'react';
import { Link } from 'react-router-dom';
import { analysisApi, ErrorStackAnalysisResponse, repositoryApi, Repository } from '../services/api';
import { useEffect } from 'react';

const ErrorStackAnalysis: React.FC = () => {
  const [errorStack, setErrorStack] = useState('');
  const [selectedRepoId, setSelectedRepoId] = useState<string>('');
  const [repositories, setRepositories] = useState<Record<string, Repository>>({});
  const [analysis, setAnalysis] = useState<ErrorStackAnalysisResponse | null>(null);
  const [aiAnalysis, setAiAnalysis] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingAi, setLoadingAi] = useState(false);
  const [loadingRepos, setLoadingRepos] = useState(true);
  
  const sampleErrorStacks = [
    {
      name: 'Nacos 示例错误',
      value: `java.lang.NullPointerException: Cannot invoke method on null object
    at com.alibaba.nacos.core.distributed.distro.task.DistroTaskEngine.addTask(DistroTaskEngine.java:89)
    at com.alibaba.nacos.core.distributed.distro.task.DistroTaskDispatch.dispatch(DistroTaskDispatch.java:67)
    at com.alibaba.nacos.core.distributed.distro.DistroProtocol.syncToAllServer(DistroProtocol.java:156)
    at com.alibaba.nacos.core.distributed.distro.DistroProtocol.onReceive(DistroProtocol.java:123)
Caused by: java.lang.IllegalStateException: Server not initialized
    at com.alibaba.nacos.core.distributed.distro.task.DistroTaskEngine.init(DistroTaskEngine.java:56)
    ... 3 more`
    },
    {
      name: 'Spring Boot 示例',
      value: `org.springframework.web.client.ResourceAccessException: I/O error on POST request
    at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:785)
    at org.springframework.web.client.RestTemplate.execute(RestTemplate.java:711)
    at org.springframework.web.client.RestTemplate.postForEntity(RestTemplate.java:478)
    at com.example.demo.service.MyService.callApi(MyService.java:42)
    at com.example.demo.controller.MyController.getData(MyController.java:28)`
    }
  ];
  
  const loadSample = (sample: string) => {
    setErrorStack(sample);
  };

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
      setAiAnalysis(null);
      const response = await analysisApi.analyzeErrorStack({
        errorStack,
        repositoryId: selectedRepoId || undefined,
      });
      setAnalysis(response.data);
      
      if (selectedRepoId) {
        await analyzeWithAI();
      }
    } catch (error) {
      console.error('Failed to analyze error stack:', error);
      alert('分析错误堆栈失败');
    } finally {
      setLoading(false);
    }
  };
  
  const analyzeWithAI = async () => {
    if (!selectedRepoId) return;
    
    try {
      setLoadingAi(true);
      const response = await analysisApi.chat({
        repositoryId: selectedRepoId,
        message: errorStack,
      });
      setAiAnalysis(response.data.message);
    } catch (error) {
      console.error('Failed to analyze with AI:', error);
    } finally {
      setLoadingAi(false);
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
        
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            快速示例
          </label>
          <div className="flex flex-wrap gap-2">
            {sampleErrorStacks.map((sample, index) => (
              <button
                key={index}
                type="button"
                onClick={() => loadSample(sample.value)}
                className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm hover:bg-gray-200 transition-colors"
              >
                {sample.name}
              </button>
            ))}
          </div>
        </div>
        
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
          <div className="flex gap-4">
            <button
              type="submit"
              disabled={!errorStack.trim() || loading}
              className="flex-1 bg-red-600 text-white py-2 px-4 rounded-lg hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? '分析中...' : '分析错误堆栈'}
            </button>
            {analysis && selectedRepoId && !aiAnalysis && (
              <button
                type="button"
                onClick={analyzeWithAI}
                disabled={loadingAi}
                className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
              >
                {loadingAi ? 'AI 分析中...' : '🤖 AI 智能分析'}
              </button>
            )}
          </div>
        </form>
      </div>

      {analysis && (
        <div className="space-y-6">
          {(aiAnalysis || loadingAi) && (
            <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg shadow-md p-6 border-2 border-blue-200">
              <div className="flex items-center gap-2 mb-4">
                <span className="text-2xl">🤖</span>
                <h2 className="text-xl font-semibold text-gray-800">AI 智能分析</h2>
              </div>
              {loadingAi ? (
                <div className="text-center py-8 text-gray-500">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
                  AI 正在分析中...
                </div>
              ) : aiAnalysis ? (
                <div className="text-gray-700 whitespace-pre-wrap font-sans">
                  {aiAnalysis}
                </div>
              ) : null}
            </div>
          )}
          
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">📊 基础分析</h2>
            
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
                      {selectedRepoId && location.filePath && location.filePath.includes('/') ? (
                        <Link
                          to={`/walkthrough/${selectedRepoId}`}
                          state={{ filePath: location.filePath }}
                          className="text-blue-600 hover:underline font-mono"
                        >
                          {location.filePath}:{location.lineNumber}
                        </Link>
                      ) : (
                        <span className="font-mono">{location.filePath}:{location.lineNumber}</span>
                      )}
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
                      {selectedRepoId && code.filePath && code.filePath.includes('/') ? (
                        <Link
                          to={`/walkthrough/${selectedRepoId}`}
                          state={{ filePath: code.filePath }}
                          className="font-medium text-blue-600 hover:underline font-mono text-sm"
                        >
                          {code.filePath}
                        </Link>
                      ) : (
                        <span className="font-medium text-gray-800 font-mono text-sm">
                          {code.filePath}
                        </span>
                      )}
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
