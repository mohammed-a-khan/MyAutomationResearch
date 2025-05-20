import React, { useState, useRef, useEffect, useCallback } from 'react';
import './Tab.css';

export interface TabItem {
  id: string;
  title: React.ReactNode;
  content: React.ReactNode;
  disabled?: boolean;
}

export interface TabsProps {
  items?: TabItem[];
  defaultActiveId?: string;
  activeId?: string;
  onChange?: (id: string) => void;
  variant?: 'horizontal' | 'vertical';
  alignment?: 'start' | 'center' | 'end' | 'stretch';
  size?: 'small' | 'medium' | 'large';
  showIndicator?: boolean;
  className?: string;
  tabClassName?: string;
  contentClassName?: string;
  animationDuration?: number;
  renderTabContent?: (tab: TabItem) => React.ReactNode;
  onTabClick?: (id: string) => void;
  children?: React.ReactNode;
  activeTab?: string;
}

// Individual Tab component for use with Tabs
export interface TabProps {
  id: string;
  label?: string;
  title?: React.ReactNode;
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  className?: string;
  children?: React.ReactNode;
  icon?: string;
}

export const Tab: React.FC<TabProps> = ({
  id,
  title,
  label,
  active = false,
  disabled = false,
  onClick,
  className = '',
  children
}) => {
  // Use label as title if title is not provided
  const displayTitle = title || label;
  
  const tabClasses = [
    'tab',
    active ? 'tab-active' : '',
    disabled ? 'tab-disabled' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <button
      id={`tab-${id}`}
      className={tabClasses}
      role="tab"
      aria-selected={active}
      aria-controls={`tabpanel-${id}`}
      tabIndex={active ? 0 : -1}
      disabled={disabled}
      onClick={disabled ? undefined : onClick}
    >
      {displayTitle}
      {children && <div style={{ display: 'none' }}>{children}</div>}
    </button>
  );
};

const Tabs: React.FC<TabsProps> = ({
  items = [],
  defaultActiveId,
  activeId: controlledActiveId,
  onChange,
  variant = 'horizontal',
  alignment = 'start',
  size = 'medium',
  showIndicator = true,
  className = '',
  tabClassName = '',
  contentClassName = '',
  animationDuration = 200,
  renderTabContent,
  onTabClick,
  children,
  activeTab
}) => {
  // If activeTab is provided, use it as controlledActiveId
  const effectiveControlledActiveId = activeTab || controlledActiveId;
  
  // Determine whether this is a controlled or uncontrolled component
  const isControlled = effectiveControlledActiveId !== undefined;

  // State for uncontrolled component
  const [activeTabId, setActiveTabId] = useState<string>(
    // If controlled, use the provided ID, otherwise use default or first tab
    isControlled 
      ? effectiveControlledActiveId 
      : (defaultActiveId || (items.length > 0 ? items[0].id : ''))
  );

  // Get the actual active ID depending on controlled/uncontrolled
  const activeId = isControlled ? effectiveControlledActiveId : activeTabId;
  
  // Refs for animation indicator
  const tabsRef = useRef<HTMLDivElement>(null);
  const indicatorRef = useRef<HTMLDivElement>(null);
  const activeTabRef = useRef<HTMLButtonElement>(null);

  const updateIndicatorPosition = useCallback(() => {
    if (!showIndicator || !tabsRef.current || !indicatorRef.current || !activeTabRef.current) return;

    const tabsRect = tabsRef.current.getBoundingClientRect();
    const activeTabRect = activeTabRef.current.getBoundingClientRect();

    if (variant === 'horizontal') {
      const left = activeTabRect.left - tabsRect.left;
      indicatorRef.current.style.left = `${left}px`;
      indicatorRef.current.style.width = `${activeTabRect.width}px`;
      indicatorRef.current.style.top = '';
      indicatorRef.current.style.height = '';
    } else {
      const top = activeTabRect.top - tabsRect.top;
      indicatorRef.current.style.top = `${top}px`;
      indicatorRef.current.style.height = `${activeTabRect.height}px`;
      indicatorRef.current.style.left = '';
      indicatorRef.current.style.width = '';
    }
  }, [showIndicator, variant]);

  // Handle tab click event
  const handleTabClick = useCallback((id: string) => {
    if (!isControlled) {
      setActiveTabId(id);
    }
    onChange?.(id);
    onTabClick?.(id);
  }, [isControlled, onChange, onTabClick]);

  // Update indicator position when active tab changes
  useEffect(() => {
    updateIndicatorPosition();
    
    // Add a small delay to ensure the DOM has updated
    const timeoutId = setTimeout(updateIndicatorPosition, 50);
    return () => clearTimeout(timeoutId);
  }, [activeId, updateIndicatorPosition]);

  // Update indicator position on window resize
  useEffect(() => {
    const handleResize = () => {
      updateIndicatorPosition();
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [updateIndicatorPosition]);

  // Compute class names
  const tabsContainerClasses = [
    'tabs-container',
    `tabs-${variant}`,
    `tabs-${size}`,
    className
  ].filter(Boolean).join(' ');

  const tabsHeaderClasses = [
    'tabs-header',
    `tabs-header-${variant}`,
    `tabs-align-${alignment}`,
  ].filter(Boolean).join(' ');

  const tabsContentClasses = [
    'tabs-content',
    contentClassName
  ].filter(Boolean).join(' ');

  // Find the currently active tab
  const activeTabItem = items.find(tab => tab.id === activeId) || items[0];

  return (
    <div className={tabsContainerClasses}>
      <div 
        ref={tabsRef}
        className={tabsHeaderClasses}
        role="tablist"
        aria-orientation={variant}
      >
        {items.map(tab => (
          <Tab
            key={tab.id}
            id={tab.id}
            title={tab.title}
            active={tab.id === activeId}
            disabled={tab.disabled}
            className={tabClassName}
            onClick={() => !tab.disabled && handleTabClick(tab.id)}
          />
        ))}
        {showIndicator && (
          <div 
            ref={indicatorRef}
            className={`tab-indicator tab-indicator-${variant}`}
            style={{ 
              transitionDuration: `${animationDuration}ms`,
              [variant === 'horizontal' ? 'bottom' : 'right']: '0'
            }}
          />
        )}
      </div>

      <div 
        className={tabsContentClasses} 
        style={{ 
          transitionDuration: `${animationDuration}ms` 
        }}
      >
        {items.map(tab => (
          <div
            key={tab.id}
            id={`tabpanel-${tab.id}`}
            role="tabpanel"
            aria-labelledby={`tab-${tab.id}`}
            className={`tab-panel ${tab.id === activeId ? 'tab-panel-active' : ''}`}
            tabIndex={0}
            hidden={tab.id !== activeId}
          >
            {renderTabContent ? renderTabContent(tab) : tab.content}
          </div>
        ))}
        {children}
      </div>
    </div>
  );
};

export default Tabs; 