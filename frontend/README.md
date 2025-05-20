# CSTestForge Frontend

This is the frontend for the CSTestForge comprehensive test automation platform. The application is built using React with TypeScript and provides a complete UI for managing test automation projects, recording tests, building test scripts, executing tests, and more.

## Features

- **Project Management**: Create, view, edit, and delete test projects
- **Test Recorder**: Record browser interactions for test creation
- **Test Builder**: Build and modify test scripts with a visual editor
- **Test Runner**: Execute and monitor test runs
- **API Testing**: Create and run API tests
- **Reports**: View detailed test reports and analytics
- **Export**: Export tests to various formats
- **Settings**: Configure platform behavior

## Technology Stack

- React 18
- TypeScript
- React Router for navigation
- Axios for API communication
- Custom CSS (no third-party UI libraries)

## Getting Started

### Prerequisites

- Node.js (v14.x or later)
- npm (v7.x or later)

### Installation

1. Clone the repository
2. Navigate to the frontend directory:
   ```
   cd CSTestForge/frontend
   ```
3. Install dependencies:
   ```
   npm install
   ```
4. Start the development server:
   ```
   npm start
   ```
5. Open [http://localhost:3000](http://localhost:3000) to view the application

## Development

### Code Structure

The codebase is organized as follows:

- `/src/components`: UI components
  - `/src/components/layout`: Layout components (Header, Sidebar, etc.)
- `/src/pages`: Page components for different sections
- `/src/services`: API service layer
- `/src/utils`: Utility functions
- `/src/types`: TypeScript types and interfaces
- `/src/styles`: Global CSS styles
- `/src/config`: Application configuration

### Build for Production

To build the application for production:

```
npm run build
```

This will create an optimized production build in the `build` folder.

## License

This project is proprietary and confidential. Unauthorized copying or distribution is prohibited. 