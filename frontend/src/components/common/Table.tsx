import React, { useState, useMemo, useCallback } from 'react';
import './Table.css';

export type SortDirection = 'asc' | 'desc' | null;

export interface Column<T> {
  key: string;
  header: string;
  render?: (value: T) => React.ReactNode;
  sortable?: boolean;
  filterable?: boolean;
  width?: string;
}

export interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (item: T) => string;
  sortable?: boolean;
  filterable?: boolean;
  selectable?: boolean;
  pagination?: boolean;
  pageSize?: number;
  onSort?: (key: string, direction: SortDirection) => void;
  onFilter?: (key: string, value: string) => void;
  onSelectionChange?: (selectedKeys: string[]) => void;
  onPageChange?: (page: number) => void;
  className?: string;
  loading?: boolean;
  error?: string;
}

function Table<T>({
  columns,
  data,
  keyExtractor,
  sortable = false,
  filterable = false,
  selectable = false,
  pagination = false,
  pageSize = 10,
  onSort,
  onFilter,
  onSelectionChange,
  onPageChange,
  className = '',
  loading = false,
  error
}: TableProps<T>) {
  const [sortConfig, setSortConfig] = useState<{ key: string; direction: SortDirection }>({
    key: '',
    direction: null
  });
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());
  const [currentPage, setCurrentPage] = useState(1);

  const handleSort = useCallback((key: string) => {
    if (!sortable) return;

    const direction: SortDirection = 
      sortConfig.key === key && sortConfig.direction === 'asc' ? 'desc' : 'asc';

    setSortConfig({ key, direction });
    onSort?.(key, direction);
  }, [sortable, sortConfig, onSort]);

  const handleFilter = useCallback((key: string, value: string) => {
    if (!filterable) return;

    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
    onFilter?.(key, value);
  }, [filterable, onFilter]);

  const handleSelectAll = useCallback((checked: boolean) => {
    if (!selectable) return;

    const newSelection = new Set<string>(checked ? data.map(item => keyExtractor(item)) : []);
    setSelectedRows(newSelection);
    onSelectionChange?.(Array.from(newSelection));
  }, [selectable, data, keyExtractor, onSelectionChange]);

  const handleSelectRow = useCallback((key: string, checked: boolean) => {
    if (!selectable) return;

    const newSelection = new Set(selectedRows);
    if (checked) {
      newSelection.add(key);
    } else {
      newSelection.delete(key);
    }
    setSelectedRows(newSelection);
    onSelectionChange?.(Array.from(newSelection));
  }, [selectable, selectedRows, onSelectionChange]);

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page);
    onPageChange?.(page);
  }, [onPageChange]);

  const filteredData = useMemo(() => {
    return data.filter(item => {
      return Object.entries(filters).every(([key, value]) => {
        if (!value) return true;
        const itemValue = String((item as any)[key]).toLowerCase();
        return itemValue.includes(value.toLowerCase());
      });
    });
  }, [data, filters]);

  const sortedData = useMemo(() => {
    if (!sortConfig.key || !sortConfig.direction) return filteredData;

    return [...filteredData].sort((a, b) => {
      const aValue = (a as any)[sortConfig.key];
      const bValue = (b as any)[sortConfig.key];

      if (aValue === bValue) return 0;
      if (aValue === null) return 1;
      if (bValue === null) return -1;

      const comparison = aValue < bValue ? -1 : 1;
      return sortConfig.direction === 'asc' ? comparison : -comparison;
    });
  }, [filteredData, sortConfig]);

  const paginatedData = useMemo(() => {
    if (!pagination) return sortedData;

    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    return sortedData.slice(start, end);
  }, [sortedData, pagination, currentPage, pageSize]);

  const totalPages = useMemo(() => {
    return Math.ceil(sortedData.length / pageSize);
  }, [sortedData.length, pageSize]);

  const tableClasses = [
    'table',
    loading ? 'table-loading' : '',
    error ? 'table-error' : '',
    className
  ].filter(Boolean).join(' ');

  if (error) {
    return (
      <div className="table-error-container">
        <div className="table-error-message">{error}</div>
      </div>
    );
  }

  return (
    <div className="table-container">
      <div className="table-wrapper">
        <table className={tableClasses}>
          <thead>
            <tr>
              {selectable && (
                <th className="table-cell-checkbox">
                  <input
                    type="checkbox"
                    checked={selectedRows.size === data.length}
                    onChange={e => handleSelectAll(e.target.checked)}
                    aria-label="Select all rows"
                  />
                </th>
              )}
              {columns.map(column => (
                <th
                  key={column.key}
                  style={{ width: column.width }}
                  className={`
                    table-cell
                    ${sortable && column.sortable ? 'table-cell-sortable' : ''}
                    ${sortConfig.key === column.key ? `table-cell-sorted-${sortConfig.direction}` : ''}
                  `}
                >
                  <div className="table-cell-content">
                    <span
                      onClick={() => handleSort(column.key)}
                      className={sortable && column.sortable ? 'table-cell-sort-trigger' : ''}
                    >
                      {column.header}
                    </span>
                    {filterable && column.filterable && (
                      <input
                        type="text"
                        className="table-cell-filter"
                        placeholder="Filter..."
                        value={filters[column.key] || ''}
                        onChange={e => handleFilter(column.key, e.target.value)}
                      />
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {paginatedData.map(item => {
              const key = keyExtractor(item);
              return (
                <tr key={key}>
                  {selectable && (
                    <td className="table-cell-checkbox">
                      <input
                        type="checkbox"
                        checked={selectedRows.has(key)}
                        onChange={e => handleSelectRow(key, e.target.checked)}
                        aria-label={`Select row ${key}`}
                      />
                    </td>
                  )}
                  {columns.map(column => (
                    <td key={column.key} className="table-cell">
                      {column.render ? column.render(item) : (item as any)[column.key]}
                    </td>
                  ))}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {pagination && totalPages > 1 && (
        <div className="table-pagination">
          <button
            className="table-pagination-button"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
          >
            Previous
          </button>
          <span className="table-pagination-info">
            Page {currentPage} of {totalPages}
          </span>
          <button
            className="table-pagination-button"
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
          >
            Next
          </button>
        </div>
      )}
      {loading && (
        <div className="table-loading-overlay">
          <div className="table-loading-spinner"></div>
        </div>
      )}
    </div>
  );
}

export default Table; 