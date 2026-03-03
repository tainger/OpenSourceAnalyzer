import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { analysisApi, ArchitectureAnalysisResponse } from '../services/api';

const ArchitectureAnalysis: React.FC = () => {
  const { repoId } = useParams<{ repoId: string }>();
  const [analysis, setAnalysis] = useState<ArchitectureAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (repoId) {
      loadAnalysis();
    }
  }, [repoId]);

  const loadAnalysis = async () => {
    try {
      setLoading(true);
      const response = await analysisApi.getArchitecture(repoId!);
      setAnalysis(response.data);
    } catch (error) {
      console.error('Failed to load architecture analysis:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="text-xl text-gray-600">加载中...</div>
      </div>
    );
  }

  if (!analysis) {
    return (
      <div className="text-center py-20">
        <p className="text-xl text-gray-600">无法加载架构分析</p>
        <Link to="/" className="text-blue-600 hover:underline mt-4 inline-block">
          返回首页
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-800">🏗️ 架构分析</h1>
        <Link to="/" className="text-blue-600 hover:underline">
          ← 返回
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4 text-gray-800">整体结构</h2>
        <p className="text-gray-700">{analysis.overallStructure}</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4 text-gray-800">主要模块</h2>
          <ul className="space-y-2">
            {analysis.mainModules.map((module, index) => (
              <li key={index} className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <div>
                  <span className="font-medium text-gray-800">{module}</span>
                  {analysis.moduleDescriptions[module] && (
                    <p className="text-sm text-gray-600">{analysis.moduleDescriptions[module]}</p>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4 text-gray-800">技术栈</h2>
          <p className="text-gray-700 mb-4">{analysis.techStack}</p>
          
          <h3 className="text-lg font-medium mb-2 text-gray-800">设计模式</h3>
          <ul className="space-y-1">
            {analysis.designPatterns.map((pattern, index) => (
              <li key={index} className="text-gray-700">• {pattern}</li>
            ))}
          </ul>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4 text-gray-800">关键文件</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {analysis.keyFiles.map((file, index) => (
            <div key={index} className="bg-gray-50 rounded px-4 py-2 text-sm text-gray-700 font-mono">
              {file}
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4 text-gray-800">建议</h2>
        <ul className="space-y-2">
          {analysis.recommendations.map((rec, index) => (
            <li key={index} className="flex items-start">
              <span className="text-green-600 mr-2">💡</span>
              <span className="text-gray-700">{rec}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default ArchitectureAnalysis;
