import React, { useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';

/**
 * NotFound component displayed when a user navigates to a non-existent route
 */
const NotFound: React.FC = () => {
  const location = useLocation();
  
  useEffect(() => {
    console.error(`404 - Not Found: Path "${location.pathname}" does not match any routes`);
  }, [location]);
  
  return (
    <div className="not-found-container">
      <div className="not-found-content">
        <h1>404</h1>
        <h2>Page Not Found</h2>
        <p>The path <code>{location.pathname}</code> does not exist or has been moved.</p>
        <p>If you believe this is an error, please check the console for debugging information.</p>
        <Link to="/" className="btn btn-primary">
          Return to Dashboard
        </Link>
      </div>
    </div>
  );
};

export default NotFound; 