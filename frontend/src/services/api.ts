import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface Repository {
  id: string;
  name: string;
  url: string;
  localPath: string;
  branch: string;
  clonedAt: string;
  lastAnalyzedAt: string | null;
  status: string;
}

export interface CloneRepositoryRequest {
  url: string;
  branch?: string;
}

export interface ArchitectureAnalysisResponse {
  repositoryId: string;
  overallStructure: string;
  mainModules: string[];
  moduleDescriptions: Record<string, string>;
  designPatterns: string[];
  keyFiles: string[];
  techStack: string;
  recommendations: string[];
}

export interface CodeSection {
  sectionName: string;
  startLine: number;
  endLine: number;
  explanation: string;
  codeSnippet: string;
}

export interface CodeWalkthroughResponse {
  repositoryId: string;
  filePath: string;
  fileSummary: string;
  sections: CodeSection[];
  dependencies: string[];
  dependents: string[];
}

export interface SuspectedLocation {
  filePath: string;
  lineNumber: number;
  className: string;
  methodName: string;
  description: string;
  confidence: number;
}

export interface RelatedCode {
  filePath: string;
  codeSnippet: string;
  relevance: string;
}

export interface ErrorStackAnalysisResponse {
  errorType: string;
  rootCause: string;
  summary: string;
  suspectedLocations: SuspectedLocation[];
  possibleFixes: string[];
  relatedCode: RelatedCode[];
}

export interface ErrorStackAnalysisRequest {
  errorStack: string;
  repositoryId?: string;
}

export interface ChatRequest {
  repositoryId: string;
  message: string;
}

export interface ChatResponse {
  message: string;
  timestamp: string;
}

export const repositoryApi = {
  clone: (data: CloneRepositoryRequest) => 
    api.post<Repository>('/repositories/clone', data),
  
  getAll: () => 
    api.get<Record<string, Repository>>('/repositories'),
  
  get: (id: string) => 
    api.get<Repository>(`/repositories/${id}`),
  
  delete: (id: string) => 
    api.delete(`/repositories/${id}`),
};

export const analysisApi = {
  getArchitecture: (repoId: string) => 
    api.get<ArchitectureAnalysisResponse>(`/analysis/architecture/${repoId}`),
  
  getWalkthrough: (repoId: string, filePath: string) => 
    api.get<CodeWalkthroughResponse>(`/analysis/walkthrough/${repoId}`, {
      params: { filePath },
    }),
  
  analyzeErrorStack: (data: ErrorStackAnalysisRequest) => 
    api.post<ErrorStackAnalysisResponse>('/analysis/error-stack', data),
  
  listFiles: (repoId: string) => 
    api.get<string[]>(`/analysis/files/${repoId}`),
  
  chat: (data: ChatRequest) => 
    api.post<ChatResponse>('/analysis/chat', data),
};

export default api;
