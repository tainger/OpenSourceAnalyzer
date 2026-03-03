import { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { analysisApi, repositoryApi, Repository } from '../services/api';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

const ChatDialog: React.FC = () => {
  const { repoId } = useParams<{ repoId: string }>();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [repository, setRepository] = useState<Repository | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (repoId) {
      loadRepository(repoId);
      addWelcomeMessage();
    }
  }, [repoId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadRepository = async (id: string) => {
    try {
      const response = await repositoryApi.get(id);
      setRepository(response.data);
    } catch (error) {
      console.error('Failed to load repository:', error);
    }
  };

  const addWelcomeMessage = () => {
    const welcome: Message = {
      id: 'welcome',
      role: 'assistant',
      content: '你好！我是 AI 代码分析助手。我可以帮你分析项目架构、查找代码文件、解释代码功能。\n\n你可以问我：\n- 这个项目的架构是什么？\n- 有哪些文件？\n- 帮我查找相关代码',
      timestamp: new Date(),
    };
    setMessages([welcome]);
  };

  const formatMessage = (content: string) => {
    return content
      .replace(/^## (.*)$/gm, '<h3 class="text-lg font-semibold text-gray-800 mt-4 mb-2">$1</h3>')
      .replace(/^### (.*)$/gm, '<h4 class="text-md font-medium text-gray-700 mt-3 mb-1">$1</h4>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\n/g, '<br>');
  };

  const handleSend = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!input.trim() || !repoId || loading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const response = await analysisApi.chat({
        repositoryId: repoId,
        message: input,
      });

      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.data.message,
        timestamp: new Date(response.data.timestamp),
      };

      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      console.error('Failed to send message:', error);
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: '抱歉，发生了错误，请稍后再试。',
        timestamp: new Date(),
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const quickQuestions = [
    '这个项目的架构是什么？',
    '有哪些文件？',
    '帮我查找相关代码',
  ];

  return (
    <div className="h-[calc(100vh-12rem)] flex flex-col bg-white rounded-lg shadow-md overflow-hidden">
      <div className="p-4 border-b bg-gray-50">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-gray-800">AI 助手</h2>
            {repository && (
              <p className="text-sm text-gray-500">
                正在分析: {repository.name}
              </p>
            )}
          </div>
          <Link
            to="/"
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            ← 返回仓库列表
          </Link>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[80%] rounded-lg p-4 ${
                message.role === 'user'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-800'
              }`}
            >
              <div
                className="prose prose-sm"
                dangerouslySetInnerHTML={{
                  __html: formatMessage(message.content),
                }}
              />
              <div
                className={`text-xs mt-2 ${
                  message.role === 'user' ? 'text-blue-200' : 'text-gray-500'
                }`}
              >
                {message.timestamp.toLocaleTimeString('zh-CN')}
              </div>
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-gray-100 text-gray-800 rounded-lg p-4">
              <div className="flex space-x-2">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }} />
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }} />
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="p-4 border-t bg-gray-50">
        <div className="flex flex-wrap gap-2 mb-4">
          {quickQuestions.map((question, index) => (
            <button
              key={index}
              onClick={() => {
                setInput(question);
                handleSend();
              }}
              className="px-3 py-1 bg-white border border-gray-300 rounded-full text-sm text-gray-700 hover:bg-gray-100 transition-colors"
            >
              {question}
            </button>
          ))}
        </div>
        <form onSubmit={handleSend} className="flex space-x-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="输入你的问题..."
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            disabled={loading}
          />
          <button
            type="submit"
            disabled={!input.trim() || loading}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            发送
          </button>
        </form>
      </div>
    </div>
  );
};

export default ChatDialog;
