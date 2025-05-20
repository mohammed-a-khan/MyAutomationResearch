import React from 'react';
import { Link } from 'react-router-dom';
import './Header.css';

export interface HeaderProps {
  toggleSidebar?: () => void;
  onToggleSidebar?: () => void;
  isMobile?: boolean;
  sidebarCollapsed?: boolean;
}

const Header: React.FC<HeaderProps> = ({ 
  toggleSidebar, 
  onToggleSidebar,
  isMobile = false,
  sidebarCollapsed = false
}) => {
  // Use onToggleSidebar if provided, otherwise fall back to toggleSidebar
  const handleToggleSidebar = () => {
    if (onToggleSidebar) {
      onToggleSidebar();
    } else if (toggleSidebar) {
      toggleSidebar();
    }
  };

  return (
    <header className="app-header">
      <div className="app-header-brand">
        <button 
          className={`sidebar-toggle ${isMobile ? 'd-block' : 'd-md-none'}`} 
          onClick={handleToggleSidebar}
          aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
            <path fillRule="evenodd" d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5z"/>
          </svg>
        </button>
        <h1 className="app-header-title">CSTestForge</h1>
      </div>
      
      <div className="app-header-actions">
        <div className="app-header-search">
          <input 
            type="text" 
            placeholder="Search tests..." 
            className="search-input"
          />
          <button className="search-button">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
            </svg>
          </button>
        </div>
        
        <div className="app-header-links">
          <a href="https://cstestforge.docs.example.com" target="_blank" rel="noopener noreferrer" className="header-link">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"/>
            </svg>
            <span>Docs</span>
          </a>
          
          <Link to="/notifications" className="header-link">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M8 16a2 2 0 0 0 2-2H6a2 2 0 0 0 2 2zm.995-14.901a1 1 0 1 0-1.99 0A5.002 5.002 0 0 0 3 6c0 1.098-.5 6-2 7h14c-1.5-1-2-5.902-2-7 0-2.42-1.72-4.44-4.005-4.901z"/>
            </svg>
            <span className="notification-badge">3</span>
          </Link>
        </div>
        
        <div className="user-profile">
          <div className="user-avatar">
            <span>AB</span>
          </div>
          <div className="user-info d-none d-md-block">
            <div className="user-name">Admin User</div>
            <div className="user-role">Administrator</div>
          </div>
          <div className="user-dropdown">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z"/>
            </svg>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header; 