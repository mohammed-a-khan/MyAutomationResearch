import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import {
  getTestStats,
  getRecentTests,
  getEnvironmentStatus,
  getMetrics,
  getExecutionSummary,
  getTestTimeline,
  getFailureAnalysis,
  getTestCoverage,
  rerunTest,
  exportDashboardReport,
  MetricsData,
  TestTimelineData,
  FailureAnalysis
} from '../services/dashboardService';
import { TestStats, RecentTest, EnvironmentStatus } from '../types/api';
import { formatError } from '../utils/errorHandling';

interface DashboardContextState {
  // States
  stats: TestStats | null;
  recentTests: RecentTest[];
  environments: EnvironmentStatus[];
  metrics: Record<string, MetricsData>;
  executionSummary: TestStats | null;
  timeline: TestTimelineData | null;
  failureAnalysis: FailureAnalysis | null;
  testCoverage: MetricsData | null;
  
  // Loading states
  loading: {
    stats: boolean;
    recentTests: boolean;
    environments: boolean;
    metrics: boolean;
    executionSummary: boolean;
    timeline: boolean;
    failureAnalysis: boolean;
    testCoverage: boolean;
  };
  
  // Error states
  errors: {
    stats: string | null;
    recentTests: string | null;
    environments: string | null;
    metrics: string | null;
    executionSummary: string | null;
    timeline: string | null;
    failureAnalysis: string | null;
    testCoverage: string | null;
  };
  
  // Methods
  fetchAllDashboardData: () => Promise<void>;
  refreshDashboardData: (section?: string) => Promise<void>;
  handleRerunTest: (testId: string) => Promise<void>;
  exportReport: (format: 'pdf' | 'csv' | 'excel') => Promise<void>;
  
  // Settings
  timeRange: number; // in days
  updateTimeRange: (days: number) => void;
  metricType: 'execution' | 'performance' | 'errors';
  updateMetricType: (type: 'execution' | 'performance' | 'errors') => void;
  period: 'day' | 'week' | 'month';
  updatePeriod: (period: 'day' | 'week' | 'month') => void;
}

const DashboardContext = createContext<DashboardContextState | undefined>(undefined);

export const DashboardProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // States
  const [stats, setStats] = useState<TestStats | null>(null);
  const [recentTests, setRecentTests] = useState<RecentTest[]>([]);
  const [environments, setEnvironments] = useState<EnvironmentStatus[]>([]);
  const [metrics, setMetrics] = useState<Record<string, MetricsData>>({});
  const [executionSummary, setExecutionSummary] = useState<TestStats | null>(null);
  const [timeline, setTimeline] = useState<TestTimelineData | null>(null);
  const [failureAnalysis, setFailureAnalysis] = useState<FailureAnalysis | null>(null);
  const [testCoverage, setTestCoverage] = useState<MetricsData | null>(null);
  
  // Settings
  const [timeRange, setTimeRange] = useState<number>(30); // Default to 30 days
  const [metricType, setMetricType] = useState<'execution' | 'performance' | 'errors'>('execution');
  const [period, setPeriod] = useState<'day' | 'week' | 'month'>('week');
  
  // Loading states (true = loading)
  const [loading, setLoading] = useState({
    stats: true,
    recentTests: true,
    environments: true,
    metrics: true,
    executionSummary: true,
    timeline: true,
    failureAnalysis: true,
    testCoverage: true
  });
  
  // Error states
  const [errors, setErrors] = useState<{
    stats: string | null;
    recentTests: string | null;
    environments: string | null;
    metrics: string | null;
    executionSummary: string | null;
    timeline: string | null;
    failureAnalysis: string | null;
    testCoverage: string | null;
  }>({
    stats: null,
    recentTests: null,
    environments: null,
    metrics: null,
    executionSummary: null,
    timeline: null,
    failureAnalysis: null,
    testCoverage: null
  });
  
  // Fetch test statistics
  const fetchTestStats = async () => {
    setLoading(prev => ({ ...prev, stats: true }));
    try {
      const data = await getTestStats();
      setStats(data);
      setErrors(prev => ({ ...prev, stats: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        stats: formatError(error, 'Failed to load test statistics') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, stats: false }));
    }
  };
  
  // Fetch recent tests
  const fetchRecentTests = async (limit: number = 5) => {
    setLoading(prev => ({ ...prev, recentTests: true }));
    try {
      const data = await getRecentTests(limit);
      setRecentTests(data);
      setErrors(prev => ({ ...prev, recentTests: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        recentTests: formatError(error, 'Failed to load recent tests') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, recentTests: false }));
    }
  };
  
  // Fetch environment status
  const fetchEnvironmentStatus = async () => {
    setLoading(prev => ({ ...prev, environments: true }));
    try {
      const data = await getEnvironmentStatus();
      setEnvironments(data);
      setErrors(prev => ({ ...prev, environments: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        environments: formatError(error, 'Failed to load environment status') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, environments: false }));
    }
  };
  
  // Fetch metrics data
  const fetchMetrics = async () => {
    setLoading(prev => ({ ...prev, metrics: true }));
    try {
      const data = await getMetrics(metricType, period);
      setMetrics(prev => ({ ...prev, [metricType]: data }));
      setErrors(prev => ({ ...prev, metrics: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        metrics: formatError(error, 'Failed to load metrics data') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, metrics: false }));
    }
  };
  
  // Fetch execution summary
  const fetchExecutionSummary = async () => {
    setLoading(prev => ({ ...prev, executionSummary: true }));
    try {
      const data = await getExecutionSummary();
      setExecutionSummary(data);
      setErrors(prev => ({ ...prev, executionSummary: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        executionSummary: formatError(error, 'Failed to load execution summary') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, executionSummary: false }));
    }
  };
  
  // Fetch test timeline data
  const fetchTestTimeline = async () => {
    setLoading(prev => ({ ...prev, timeline: true }));
    try {
      const data = await getTestTimeline(timeRange);
      setTimeline(data);
      setErrors(prev => ({ ...prev, timeline: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        timeline: formatError(error, 'Failed to load test timeline') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, timeline: false }));
    }
  };
  
  // Fetch failure analysis data
  const fetchFailureAnalysis = async () => {
    setLoading(prev => ({ ...prev, failureAnalysis: true }));
    try {
      const data = await getFailureAnalysis(timeRange);
      setFailureAnalysis(data);
      setErrors(prev => ({ ...prev, failureAnalysis: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        failureAnalysis: formatError(error, 'Failed to load failure analysis') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, failureAnalysis: false }));
    }
  };
  
  // Fetch test coverage data
  const fetchTestCoverage = async () => {
    setLoading(prev => ({ ...prev, testCoverage: true }));
    try {
      const data = await getTestCoverage();
      setTestCoverage(data);
      setErrors(prev => ({ ...prev, testCoverage: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        testCoverage: formatError(error, 'Failed to load test coverage') 
      }));
    } finally {
      setLoading(prev => ({ ...prev, testCoverage: false }));
    }
  };
  
  // Fetch all dashboard data
  const fetchAllDashboardData = useCallback(async () => {
    await Promise.all([
      fetchTestStats(),
      fetchRecentTests(),
      fetchEnvironmentStatus(),
      fetchMetrics(),
      fetchExecutionSummary(),
      fetchTestTimeline(),
      fetchFailureAnalysis(),
      fetchTestCoverage()
    ]);
  }, [metricType, period, timeRange]);
  
  // Refresh specific dashboard data
  const refreshDashboardData = async (section?: string) => {
    if (!section) {
      await fetchAllDashboardData();
      return;
    }
    
    switch (section) {
      case 'stats':
        await fetchTestStats();
        break;
      case 'recentTests':
        await fetchRecentTests();
        break;
      case 'environments':
        await fetchEnvironmentStatus();
        break;
      case 'metrics':
        await fetchMetrics();
        break;
      case 'executionSummary':
        await fetchExecutionSummary();
        break;
      case 'timeline':
        await fetchTestTimeline();
        break;
      case 'failureAnalysis':
        await fetchFailureAnalysis();
        break;
      case 'testCoverage':
        await fetchTestCoverage();
        break;
      default:
        await fetchAllDashboardData();
    }
  };
  
  // Handle test rerun
  const handleRerunTest = async (testId: string) => {
    try {
      // Optimistically update UI
      setRecentTests(prev => 
        prev.map(test => 
          test.id === testId 
            ? { ...test, status: 'running' } 
            : test
        )
      );
      
      // Make API call to rerun the test
      await rerunTest(testId);
      
      // Refresh data after a short delay to get updated status
      setTimeout(async () => {
        await fetchRecentTests();
      }, 1000);
    } catch (error) {
      // Revert optimistic update
      await fetchRecentTests();
      console.error('Error rerunning test:', error);
    }
  };
  
  // Export dashboard report
  const exportReport = async (format: 'pdf' | 'csv' | 'excel') => {
    try {
      await exportDashboardReport(format);
    } catch (error) {
      console.error('Error exporting report:', error);
    }
  };
  
  // Update settings
  const updateTimeRange = (days: number) => {
    setTimeRange(days);
  };
  
  const updateMetricType = (type: 'execution' | 'performance' | 'errors') => {
    setMetricType(type);
  };
  
  const updatePeriod = (p: 'day' | 'week' | 'month') => {
    setPeriod(p);
  };
  
  // Load dashboard data on mount and when settings change
  useEffect(() => {
    fetchAllDashboardData();
    
    // Set up polling interval for real-time updates (every 30 seconds)
    const pollingInterval = setInterval(fetchAllDashboardData, 30000);
    
    // Clean up interval on component unmount
    return () => clearInterval(pollingInterval);
  }, [fetchAllDashboardData, timeRange, metricType, period]);
  
  // Expose context values
  const value: DashboardContextState = {
    // States
    stats,
    recentTests,
    environments,
    metrics,
    executionSummary,
    timeline,
    failureAnalysis,
    testCoverage,
    
    // Loading states
    loading,
    
    // Error states
    errors,
    
    // Methods
    fetchAllDashboardData,
    refreshDashboardData,
    handleRerunTest,
    exportReport,
    
    // Settings
    timeRange,
    updateTimeRange,
    metricType,
    updateMetricType,
    period,
    updatePeriod
  };
  
  return (
    <DashboardContext.Provider value={value}>
      {children}
    </DashboardContext.Provider>
  );
};

// Custom hook for accessing dashboard context
export const useDashboard = () => {
  const context = useContext(DashboardContext);
  if (context === undefined) {
    throw new Error('useDashboard must be used within a DashboardProvider');
  }
  return context;
}; 