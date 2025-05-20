import React, { useState, useCallback } from 'react';
import './Card.css';

export type CardStatus = 'default' | 'success' | 'warning' | 'error' | 'info';

export interface CardAction {
  label: string;
  icon?: React.ReactNode;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
}

export interface CardProps {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  children: React.ReactNode;
  footer?: React.ReactNode;
  actions?: CardAction[];
  status?: CardStatus;
  statusText?: string;
  collapsible?: boolean;
  defaultCollapsed?: boolean;
  className?: string;
  headerClassName?: string;
  bodyClassName?: string;
  footerClassName?: string;
  bordered?: boolean;
  elevated?: boolean;
  width?: string;
  onCollapse?: (collapsed: boolean) => void;
}

interface CardHeaderProps {
  children: React.ReactNode;
  className?: string;
}

interface CardBodyProps {
  children: React.ReactNode;
  className?: string;
}

interface CardFooterProps {
  children: React.ReactNode;
  className?: string;
}

// Define the compound component type
interface CardComponent extends React.FC<CardProps> {
  Header: React.FC<CardHeaderProps>;
  Body: React.FC<CardBodyProps>;
  Footer: React.FC<CardFooterProps>;
}

const CardHeader: React.FC<CardHeaderProps> = ({ children, className = '' }) => {
  return (
    <div className={`card-header ${className}`}>
      {children}
    </div>
  );
};

const CardBody: React.FC<CardBodyProps> = ({ children, className = '' }) => {
  return (
    <div className={`card-body ${className}`}>
      {children}
    </div>
  );
};

const CardFooter: React.FC<CardFooterProps> = ({ children, className = '' }) => {
  return (
    <div className={`card-footer ${className}`}>
      {children}
    </div>
  );
};

const Card: CardComponent = ({
  title,
  subtitle,
  children,
  footer,
  actions = [],
  status = 'default',
  statusText,
  collapsible = false,
  defaultCollapsed = false,
  className = '',
  headerClassName = '',
  bodyClassName = '',
  footerClassName = '',
  bordered = true,
  elevated = false,
  width,
  onCollapse
}) => {
  const [collapsed, setCollapsed] = useState(defaultCollapsed);

  const handleToggleCollapse = useCallback(() => {
    const newCollapsed = !collapsed;
    setCollapsed(newCollapsed);
    onCollapse?.(newCollapsed);
  }, [collapsed, onCollapse]);

  // Construct class names
  const cardClasses = [
    'card',
    bordered ? 'card-bordered' : '',
    elevated ? 'card-elevated' : '',
    `card-status-${status}`,
    className
  ].filter(Boolean).join(' ');

  const cardHeaderClasses = [
    'card-header',
    collapsible ? 'card-header-collapsible' : '',
    headerClassName
  ].filter(Boolean).join(' ');

  const cardBodyClasses = [
    'card-body',
    collapsed ? 'card-body-collapsed' : '',
    bodyClassName
  ].filter(Boolean).join(' ');

  const cardFooterClasses = [
    'card-footer',
    footerClassName
  ].filter(Boolean).join(' ');

  const hasHeader = title || subtitle || status !== 'default' || collapsible;
  const hasFooter = footer || actions.length > 0;

  return (
    <div className={cardClasses} style={{ width }}>
      {status !== 'default' && (
        <div className={`card-status-indicator card-status-indicator-${status}`} />
      )}
      
      {hasHeader && (
        <div 
          className={cardHeaderClasses} 
          onClick={collapsible ? handleToggleCollapse : undefined}
          style={collapsible ? { cursor: 'pointer' } : undefined}
        >
          <div className="card-header-content">
            {title && (
              <h3 className="card-title">
                {title}
              </h3>
            )}
            {subtitle && (
              <div className="card-subtitle">
                {subtitle}
              </div>
            )}
            {statusText && (
              <div className={`card-status-text card-status-text-${status}`}>
                {statusText}
              </div>
            )}
          </div>

          {collapsible && (
            <button 
              className="card-collapse-toggle" 
              onClick={e => {
                e.stopPropagation();
                handleToggleCollapse();
              }}
              aria-expanded={!collapsed}
              aria-label={collapsed ? 'Expand card' : 'Collapse card'}
            >
              <svg 
                className={`card-collapse-icon ${collapsed ? 'card-collapse-icon-collapsed' : ''}`}
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path 
                  d="M19 9L12 16L5 9" 
                  stroke="currentColor" 
                  strokeWidth="2" 
                  strokeLinecap="round" 
                  strokeLinejoin="round"
                />
              </svg>
            </button>
          )}
        </div>
      )}

      <div className={cardBodyClasses}>
        {children}
      </div>

      {hasFooter && !collapsed && (
        <div className={cardFooterClasses}>
          {footer}
          
          {actions.length > 0 && (
            <div className="card-actions">
              {actions.map((action, index) => (
                <button
                  key={index}
                  className={`card-action card-action-${action.variant || 'secondary'}`}
                  onClick={action.onClick}
                  disabled={action.disabled}
                >
                  {action.icon && <span className="card-action-icon">{action.icon}</span>}
                  {action.label}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// Attach sub-components
Card.Header = CardHeader;
Card.Body = CardBody;
Card.Footer = CardFooter;

export default Card; 