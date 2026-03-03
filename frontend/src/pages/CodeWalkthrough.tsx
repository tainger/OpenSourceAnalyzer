import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { analysisApi, CodeWalkthroughResponse } from '../services/api';

interface TreeNode {
  name: string;
  path: string;
  isDirectory: boolean;
  children?: TreeNode[];
}

const buildTree = (files: string[]): TreeNode[] => {
  const root: Record<string, TreeNode> = {};
  
  files.forEach(filePath => {
    const parts = filePath.split('/');
    let current: Record<string, TreeNode> = root;
    
    parts.forEach((part, index) => {
      const isLast = index === parts.length - 1;
      const path = parts.slice(0, index + 1).join('/');
      
      if (!current[part]) {
        current[part] = {
          name: part,
          path,
          isDirectory: !isLast,
          children: isLast ? undefined : {}
        };
      }
      
      if (!isLast && current[part].children) {
        current = current[part].children as Record<string, TreeNode>;
      }
    });
  });
  
  const convertToArray = (nodes: Record<string, TreeNode>): TreeNode[] => {
    return Object.values(nodes)
      .sort((a, b) => {
        if (a.isDirectory && !b.isDirectory) return -1;
        if (!a.isDirectory && b.isDirectory) return 1;
        return a.name.localeCompare(b.name);
      })
      .map(node => ({
        ...node,
        children: node.children ? convertToArray(node.children as Record<string, TreeNode>) : undefined
      }));
  };
  
  return convertToArray(root);
};

const TreeItem: React.FC<{
  node: TreeNode;
  selectedFile: string | null;
  onSelect: (path: string) => void;
  level: number;
}> = ({ node, selectedFile, onSelect, level }) => {
  const [expanded, setExpanded] = useState(true);
  
  const handleToggle = () => {
    if (node.isDirectory) {
      setExpanded(!expanded);
    } else {
      onSelect(node.path);
    }
  };
  
  return (
    <div>
      <button
        onClick={handleToggle}
        className={`w-full text-left px-2 py-1 rounded text-sm hover:bg-blue-50 text-gray-700 hover:text-blue-700 font-mono truncate flex items-center ${
          selectedFile === node.path ? 'bg-blue-100 text-blue-800' : ''
        }`}
        style={{ paddingLeft: `${level * 16 + 8}px` }}
      >
        {node.isDirectory && (
          <span className="mr-1 text-gray-400">
            {expanded ? '📂' : '📁'}
          </span>
        )}
        {!node.isDirectory && <span className="mr-1 text-gray-400">📄</span>}
        {node.name}
      </button>
      
      {node.isDirectory && expanded && node.children && (
        <div>
          {node.children.map((child, index) => (
            <TreeItem
              key={`${child.path}-${index}`}
              node={child}
              selectedFile={selectedFile}
              onSelect={onSelect}
              level={level + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
};

const CodeWalkthrough: React.FC = () => {
  const { repoId } = useParams<{ repoId: string }>();
  const [files, setFiles] = useState<string[]>([]);
  const [fileTree, setFileTree] = useState<TreeNode[]>([]);
  const [filteredTree, setFilteredTree] = useState<TreeNode[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [walkthrough, setWalkthrough] = useState<CodeWalkthroughResponse | null>(null);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [loadingWalkthrough, setLoadingWalkthrough] = useState(false);

  useEffect(() => {
    if (repoId) {
      loadFiles();
    }
  }, [repoId]);

  useEffect(() => {
    if (searchQuery.trim() === '') {
      setFilteredTree(fileTree);
    } else {
      const filteredFiles = files.filter(file => 
        file.toLowerCase().includes(searchQuery.toLowerCase())
      );
      setFilteredTree(buildTree(filteredFiles));
    }
  }, [searchQuery, fileTree, files]);

  const loadFiles = async () => {
    try {
      setLoadingFiles(true);
      const response = await analysisApi.listFiles(repoId!);
      const fileList = response.data;
      setFiles(fileList);
      const tree = buildTree(fileList);
      setFileTree(tree);
      setFilteredTree(tree);
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
            <h2 className="text-xl font-semibold mb-4 text-gray-800">文件列表 ({files.length} 个文件)</h2>
            <div className="mb-4">
              <input
                type="text"
                placeholder="搜索文件..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            {loadingFiles ? (
              <div className="text-center py-8 text-gray-500">加载中...</div>
            ) : (
              <div className="max-h-96 overflow-y-auto">
                {filteredTree.map((node, index) => (
                  <TreeItem
                    key={`${node.path}-${index}`}
                    node={node}
                    selectedFile={selectedFile}
                    onSelect={loadWalkthrough}
                    level={0}
                  />
                ))}
                {filteredTree.length === 0 && searchQuery && (
                  <div className="text-center py-8 text-gray-500">未找到匹配的文件</div>
                )}
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
