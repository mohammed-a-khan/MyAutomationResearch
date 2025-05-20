import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import StatusBadge from './StatusBadge';
import { TestStatus } from '../../types/execution';
import './Execution.css';

// Mock interfaces for tests, to be replaced with actual API types
interface Test {
  id: string;
  name: string;
  description?: string;
  lastRun?: {
    status: TestStatus;
    date: string;
  };
  tags: string[];
  suiteId?: string;
}

interface TestSuite {
  id: string;
  name: string;
}

interface TestSelectorProps {
  projectId: string;
  onTestsSelected: (testIds: string[]) => void;
  isLoading?: boolean;
}

/**
 * TestSelector component for selecting tests to execute
 */
const TestSelector: React.FC<TestSelectorProps> = ({
  projectId,
  onTestsSelected,
  isLoading = false
}) => {
  // State
  const [tests, setTests] = useState<Test[]>([]);
  const [suites, setSuites] = useState<TestSuite[]>([]);
  const [selectedTests, setSelectedTests] = useState<string[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSuite, setSelectedSuite] = useState<string>('');
  const [selectedTag, setSelectedTag] = useState<string>('');
  const [availableTags, setAvailableTags] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load tests for the project
  useEffect(() => {
    const fetchTests = async () => {
      setLoading(true);
      setError(null);
      
      try {
        // In a real implementation, this would call the API
        // const response = await testService.getTests(projectId);
        // setTests(response.data);
        
        // Mock data for demonstration
        const mockTests: Test[] = [
          {
            id: '1',
            name: 'Login Test',
            description: 'Verifies user login functionality',
            lastRun: { status: TestStatus.PASSED, date: '2023-07-15T14:30:00Z' },
            tags: ['login', 'smoke', 'critical']
          },
          {
            id: '2',
            name: 'Registration Flow',
            description: 'Tests the user registration process',
            lastRun: { status: TestStatus.FAILED, date: '2023-07-14T09:15:00Z' },
            tags: ['registration', 'regression']
          },
          {
            id: '3',
            name: 'Product Search',
            description: 'Validates product search functionality',
            lastRun: { status: TestStatus.PASSED, date: '2023-07-13T11:45:00Z' },
            tags: ['search', 'regression']
          },
          {
            id: '4',
            name: 'Checkout Process',
            description: 'End-to-end test of checkout workflow',
            lastRun: { status: TestStatus.SKIPPED, date: '2023-07-12T16:20:00Z' },
            tags: ['checkout', 'e2e', 'critical']
          },
        ];
        
        setTests(mockTests);
        
        // Extract unique tags
        const tags = Array.from(
          new Set(mockTests.flatMap(test => test.tags))
        ).sort();
        
        setAvailableTags(tags);
        
        // Mock suites
        const mockSuites: TestSuite[] = [
          { id: 'smoke', name: 'Smoke Tests' },
          { id: 'regression', name: 'Regression Tests' },
          { id: 'e2e', name: 'End-to-End Tests' }
        ];
        
        setSuites(mockSuites);
      } catch (err) {
        setError('Failed to load tests. Please try again.');
        console.error('Error loading tests:', err);
      } finally {
        setLoading(false);
      }
    };
    
    fetchTests();
  }, [projectId]);
  
  // Filter tests based on search term, selected suite, and tag
  const filteredTests = tests.filter(test => {
    // Search term filter
    const searchMatch = !searchTerm || 
      test.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
      (test.description && test.description.toLowerCase().includes(searchTerm.toLowerCase()));
    
    // Suite filter
    const suiteMatch = !selectedSuite || test.suiteId === selectedSuite;
    
    // Tag filter
    const tagMatch = !selectedTag || test.tags.includes(selectedTag);
    
    return searchMatch && suiteMatch && tagMatch;
  });
  
  // Toggle test selection
  const toggleTestSelection = (testId: string) => {
    setSelectedTests(prev => {
      if (prev.includes(testId)) {
        return prev.filter(id => id !== testId);
      } else {
        return [...prev, testId];
      }
    });
  };
  
  // Select/deselect all visible tests
  const toggleSelectAll = () => {
    if (selectedTests.length === filteredTests.length) {
      // Deselect all if all are selected
      setSelectedTests([]);
    } else {
      // Select all filtered tests
      setSelectedTests(filteredTests.map(test => test.id));
    }
  };
  
  // Handle run tests button click
  const handleRunTests = () => {
    if (selectedTests.length > 0) {
      onTestsSelected(selectedTests);
    }
  };
  
  // Reset filters
  const resetFilters = () => {
    setSearchTerm('');
    setSelectedSuite('');
    setSelectedTag('');
  };
  
  // Format date to relative time
  const formatRelativeTime = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
    
    if (diffInSeconds < 60) return 'just now';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
    if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} days ago`;
    
    return date.toLocaleDateString();
  };
  
  const areAllSelected = filteredTests.length > 0 && 
    filteredTests.every(test => selectedTests.includes(test.id));
  
  return (
    <div className="test-selector">
      <div className="test-selector-filters">
        <div className="row mb-3">
          <div className="col-md-6 mb-2 mb-md-0">
            <Input
              placeholder="Search tests..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              leftIcon={<i className="bi bi-search"></i>}
              disabled={isLoading || loading}
            />
          </div>
          <div className="col-md-3 mb-2 mb-md-0">
            <Select
              value={selectedSuite}
              onChange={(e) => setSelectedSuite(e.target.value)}
              options={[
                { value: '', label: 'All Suites' },
                ...suites.map(suite => ({
                  value: suite.id,
                  label: suite.name
                }))
              ]}
              disabled={isLoading || loading}
            />
          </div>
          <div className="col-md-3">
            <Select
              value={selectedTag}
              onChange={(e) => setSelectedTag(e.target.value)}
              options={[
                { value: '', label: 'All Tags' },
                ...availableTags.map(tag => ({
                  value: tag,
                  label: tag
                }))
              ]}
              disabled={isLoading || loading}
            />
          </div>
        </div>
        
        <div className="d-flex justify-content-between">
          <div>
            <Button
              variant="outline"
              size="sm"
              onClick={resetFilters}
              disabled={isLoading || loading || (!searchTerm && !selectedSuite && !selectedTag)}
            >
              <i className="bi bi-x-circle me-1"></i> Clear Filters
            </Button>
          </div>
          <div>
            <Button
              variant="outline"
              size="sm"
              onClick={toggleSelectAll}
              disabled={isLoading || loading || filteredTests.length === 0}
            >
              {areAllSelected ? (
                <>
                  <i className="bi bi-square me-1"></i> Deselect All
                </>
              ) : (
                <>
                  <i className="bi bi-check-square me-1"></i> Select All
                </>
              )}
            </Button>
          </div>
        </div>
      </div>
      
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      
      <div className="test-selector-list">
        {loading ? (
          <div className="d-flex justify-content-center align-items-center h-100">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : filteredTests.length === 0 ? (
          <div className="test-selector-empty">
            <i className="bi bi-search mb-2" style={{ fontSize: '24px' }}></i>
            <p>No tests match your filter criteria.</p>
            <p>Try adjusting your filters or search term.</p>
          </div>
        ) : (
          filteredTests.map(test => (
            <div 
              key={test.id} 
              className="test-item"
              onClick={() => toggleTestSelection(test.id)}
            >
              <div className="test-item-checkbox">
                <input
                  type="checkbox"
                  checked={selectedTests.includes(test.id)}
                  onChange={() => {}}
                  onClick={(e) => e.stopPropagation()}
                />
              </div>
              <div className="test-item-details">
                <div className="test-item-name">{test.name}</div>
                {test.description && (
                  <div className="text-muted small">{test.description}</div>
                )}
                <div className="test-item-info">
                  {test.lastRun && (
                    <>
                      <div className="d-flex align-items-center gap-1">
                        <StatusBadge status={test.lastRun.status} />
                      </div>
                      <div>
                        <i className="bi bi-clock me-1"></i>
                        {formatRelativeTime(test.lastRun.date)}
                      </div>
                    </>
                  )}
                  {test.tags.length > 0 && (
                    <div className="test-item-tags">
                      {test.tags.map(tag => (
                        <span key={tag} className="test-item-tag">
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
      
      <div className="test-selector-footer">
        <div className="test-selector-summary">
          {selectedTests.length} of {filteredTests.length} tests selected
        </div>
        <div>
          <Button
            onClick={handleRunTests}
            disabled={isLoading || loading || selectedTests.length === 0}
          >
            <i className="bi bi-play-fill me-1"></i> Run Selected Tests
          </Button>
        </div>
      </div>
    </div>
  );
};

export default TestSelector; 