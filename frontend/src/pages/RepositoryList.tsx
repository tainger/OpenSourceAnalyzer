import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { repositoryApi, Repository } from '../services/api';

interface RepositoryListProps {
  onSelectRepo: (repoId: string) => void;
  selectedRepo: string | null;
}

const RepositoryList: React.FC<RepositoryListProps> = ({ onSelectRepo, selectedRepo }) => {
  const [repositories, setRepositories] = useState<Record<string, Repository>>({});
  const [url, setUrl] = useState('');
  const [branch, setBranch] = useState('');
  const [loading, setLoading] = useState(false);
  const [cloning, setCloning] = useState(false);

  useEffect(() => {
    loadRepositories();
  }, []);

  const loadRepositories = async () => {
    try {
      setLoading(true);
      const response = await repositoryApi.getAll();
      setRepositories(response.data);
    } catch (error) {
      console.error('Failed to load repositories:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleClone = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!url) return;

    try {
      setCloning(true);
      await repositoryApi.clone({ url, branch: branch || undefined });
      setUrl('');
      setBranch('');
      loadRepositories();
    } catch (error) {
      console.error('Failed to clone repository:', error);
      alert('克隆仓库失败，请检查URL是否正确');
    } finally {
      setCloning(false);
    }
  };

  const handleDelete = async (repoId: string) => {
    if (confirm('确定要删除这个仓库吗？')) {
      try {
        await repositoryApi.delete(repoId);
        loadRepositories();
      } catch (error) {
        console.error('Failed to delete repository:', error);
      }
    }
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      COMPLETED: 'bg-green-100 text-green-800',
      CLONING: 'bg-blue-100 text-blue-800',
      FAILED: 'bg-red-100 text-red-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  };

  const renderRepoItem = (repo: Repository) => {
    const isSelected = selectedRepo === repo.id;
    const isCompleted = repo.status === 'COMPLETED';
    
    return (
      <div
        key={repo.id}
        className={`border rounded-lg p-4 cursor-pointer transition-colors ${
          isSelected ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'
        }`}
        onClick={() => onSelectRepo(repo.id)}
      >
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-800">{repo.name}</h3>
            <p className="text-sm text-gray-500">{repo.url}</p>
            <p className="text-xs text-gray-400 mt-1">
              克隆于: {new Date(repo.clonedAt).toLocaleString('zh-CN')}
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(repo.status)}`}>
              {repo.status}
            </span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleDelete(repo.id);
              }}
              className="text-red-600 hover:text-red-800"
            >
              删除
            </button>
          </div>
        </div>
        {isSelected && isCompleted && (
          <div className="mt-4 flex space-x-2">
            <Link
              to={`/architecture/${repo.id}`}
              className="flex-1 bg-purple-600 text-white py-2 px-4 rounded-lg text-center hover:bg-purple-700 transition-colors"
            >
              🏗️ 架构分析
            </Link>
            <Link
              to={`/walkthrough/${repo.id}`}
              className="flex-1 bg-green-600 text-white py-2 px-4 rounded-lg text-center hover:bg-green-700 transition-colors"
            >
              📖 源码走读
            </Link>
            <Link
              to={`/chat/${repo.id}`}
              className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-lg text-center hover:bg-blue-700 transition-colors"
            >
              🤖 AI 助手
            </Link>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">克隆新仓库</h2>
        <form onSubmit={handleClone} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              GitHub 仓库 URL
            </label>
            <input
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://github.com/username/repository.git"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={cloning}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              分支 (可选，默认 main)
            </label>
            <input
              type="text"
              value={branch}
              onChange={(e) => setBranch(e.target.value)}
              placeholder="main / master"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={cloning}
            />
          </div>
          <button
            type="submit"
            disabled={!url || cloning}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {cloning ? '克隆中...' : '克隆并分析'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">已克隆的仓库</h2>
        {loading ? (
          <div className="text-center py-8 text-gray-500">加载中...</div>
        ) : Object.keys(repositories).length === 0 ? (
          <div className="text-center py-8 text-gray-500">暂无仓库，请先克隆一个仓库</div>
        ) : (
          <div className="space-y-4">
            {Object.values(repositories).map(renderRepoItem)}
          </div>
        )}
      </div>
    </div>
  );
};

export default RepositoryList;
