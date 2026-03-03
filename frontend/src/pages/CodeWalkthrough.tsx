import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { analysisApi, CodeWalkthroughResponse } from '../services/api';

const CodeWalkthrough: React.FC = () => {
  const { repoId } = useParams<{ repoId: string }>();
  const [files, setFiles] = useState<string[]>([]);
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [walkthrough, setWalkthrough] = useState<CodeWalkthroughResponse | null>(null);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [loadingWalkthrough, setLoadingWalkthrough] = useState(false);

  useEffect(() => {
    if (repoId) {
      loadFiles();
    }
  }, [repoId]);

  const loadFiles = async () => {
    try {
      setLoadingFiles(true);
      const response = await analysisApi.listFiles(repoId!);
      setFiles(response.data);
    } catch (error) {
      console.error('Failed to load files:', error);
    } finally {
      setLoadingFiles(false);
    }
  };

  const loadWalkthrough = async (filePath: string) => {
    try {
      setLoadingWalkthrough(true);
      setSelectedFile(filePath);
      const response = await analysisApi.getWalkthrough(repoId!, filePath);
      setWalkthrough(response.data);
    } catch (error) {
      console.error('Failed to load walkthrough:', error);
    } finally {
      setLoadingWalkthrough(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-800">📖 源码走读</h1>
        <Link to="/" className="text-blue-600 hover:underline">
          ← 返回
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">文件列表</h2>
            {loadingFiles ? (
              <div className="text-center py-8 text-gray-500">加载中...</div>
            ) : (
              <div className="max-h-96 overflow-y-auto space-y-1">
                {files.map((file, index) => (
                  <button
                    key={index}
                    onClick={() => loadWalkthrough(file)}
                    className={`w-full text-left px-3 py-2 rounded text-sm hover:bg-blue-50 text-gray-700 hover:text-blue-700 font-mono truncate ${
                      selectedFile === file ? 'bg-blue-100 text-blue-800' : ''
                    }`}
                  >
                    {file}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="lg:col-span-2">
          {loadingWalkthrough ? (
            <div className="bg-white rounded-lg shadow-md p-6">
              <div className="text-center py-20 text-gray-500">加载中...</div>
            </div>
          ) : !walkthrough ? (
            <div className="bg-white rounded-lg shadow-md p-6">
              <p className="text-center text-gray-500">请选择一个文件查看源码走读</p>
            </div>
          ) : (
            <div className="space-y-6">
              <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-xl font-semibold mb-4 text-gray-800">
                  {walkthrough.filePath}
                </h2>
                <p className="text-gray-700">{walkthrough.fileSummary}</p>
              </div>

              {walkthrough.dependencies.length > 0 && (
                <div className="bg-white rounded-lg shadow-md p-6">
                  <h3 className="text-lg font-medium mb-3 text-gray-800">依赖</h3>
                  <div className="flex flex-wrap gap-2">
                    {walkthrough.dependencies.map((dep, index) => (
                      <span
                        key={index}
                        className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm"
                      >
                        {dep}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              <div className="space-y-4">
                {walkthrough.sections.map((section, index) => (
                  <div key={index} className="bg-white rounded-lg shadow-md p-6">
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="text-lg font-medium text-gray-800">
                        {section.sectionName}
                      </h3>
                      <span className="text-sm text-gray-500">
                        行 {section.startLine}-{section.endLine}
                      </span>
                    </div>
                    <p className="text-gray-700 mb-3">{section.explanation}</p>
                    <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto text-sm">
                      <code>{section.codeSnippet}</code>
                    </pre>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CodeWalkthrough;
