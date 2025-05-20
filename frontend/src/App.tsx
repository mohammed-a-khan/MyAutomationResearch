import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation, useNavigationType } from 'react-router-dom';
import Layout from './components/layout/Layout';
import DashboardPanel from './components/dashboard/DashboardPanel';
import RecorderPanel from './components/recorder/RecorderPanel';
import { RecorderProvider } from './context/RecorderContext';
import { ProjectProvider } from './context/ProjectContext';
import { CodeBuilderProvider } from './context/CodeBuilderContext';
import { ExecutionProvider } from './context/ExecutionContext';
import { ApiTestingProvider } from './context/ApiTestingContext';
import { ExportProvider } from './context/ExportContext';
import { AdoIntegrationProvider } from './context/AdoIntegrationContext';
import {
  ProjectList,
  ProjectDetail,
  ProjectEditForm,
  ProjectSettings
} from './components/project';
import EnvironmentEditor from './components/project/EnvironmentEditor';
import CodeBuilderPanel from './components/codebuilder/CodeBuilderPanel';
import ExecutionPanel from './components/execution/ExecutionPanel';
import ApiTestingPanel from './components/api/ApiTestingPanel';
import ExportPanel from './components/export/ExportPanel';
import TestRunnerPanel from './components/runner/TestRunnerPanel';
import NotificationsPanel from './components/notifications/NotificationsPanel';
import SettingsPanel from './components/settings/SettingsPanel';
import AdoIntegrationPanel from './components/ado/AdoIntegrationPanel';
import NotFound from './components/common/NotFound';

// Import styles
import './styles/index.css';

// RouteLogger component for debugging
const RouteLogger = () => {
  const location = useLocation();
  const navigationType = useNavigationType();

  useEffect(() => {
    console.log('Route changed:', {
      pathname: location.pathname,
      navigationType,
      search: location.search,
      hash: location.hash,
      key: location.key,
      state: location.state
    });
  }, [location, navigationType]);

  return null;
};

const App: React.FC = () => {
  return (
      <Router basename="/cstestforge">
        <RouteLogger />
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<DashboardPanel />} />

            {/* Project Management Routes */}
            <Route path="/projects" element={
              <ProjectProvider>
                <ProjectList />
              </ProjectProvider>
            } />
            <Route path="/projects/:projectId" element={
              <ProjectProvider>
                <ProjectDetail />
              </ProjectProvider>
            } />
            <Route path="/projects/:projectId/edit" element={
              <ProjectProvider>
                <ProjectEditForm />
              </ProjectProvider>
            } />
            <Route path="/projects/:projectId/settings" element={
              <ProjectProvider>
                <ProjectSettings />
              </ProjectProvider>
            } />
            <Route path="/projects/:projectId/environments/create" element={
              <ProjectProvider>
                <EnvironmentEditor isNew={true} />
              </ProjectProvider>
            } />
            <Route path="/projects/:projectId/environments/:environmentId" element={
              <ProjectProvider>
                <EnvironmentEditor />
              </ProjectProvider>
            } />

            {/* Recorder Routes */}
            <Route path="/recorder" element={
              <ProjectProvider>
                <RecorderProvider>
                  <RecorderPanel />
                </RecorderProvider>
              </ProjectProvider>
            } />

            {/* Add project-specific recorder route */}
            <Route path="/recorder/:projectId" element={
              <ProjectProvider>
                <RecorderProvider>
                  <RecorderPanel />
                </RecorderProvider>
              </ProjectProvider>
            } />

            {/* Code Builder Routes */}
            <Route path="/builder" element={
              <CodeBuilderProvider>
                <CodeBuilderPanel />
              </CodeBuilderProvider>
            } />
            <Route path="/builder/:projectId" element={
              <CodeBuilderProvider>
                <CodeBuilderPanel />
              </CodeBuilderProvider>
            } />

            {/* Test Execution Routes */}
            <Route path="/execution" element={
              <ExecutionProvider>
                <ExecutionPanel />
              </ExecutionProvider>
            } />
            <Route path="/execution/:projectId" element={
              <ExecutionProvider>
                <ExecutionPanel />
              </ExecutionProvider>
            } />

            {/* Test Runner Routes */}
            <Route path="/runner" element={
              <ExecutionProvider>
                <TestRunnerPanel />
              </ExecutionProvider>
            } />
            <Route path="/runner/:projectId" element={
              <ExecutionProvider>
                <TestRunnerPanel />
              </ExecutionProvider>
            } />

            {/* API Testing Routes */}
            <Route path="/api-testing" element={
              <ApiTestingProvider>
                <ApiTestingPanel />
              </ApiTestingProvider>
            } />
            <Route path="/api-testing/:projectId" element={
              <ApiTestingProvider>
                <ApiTestingPanel />
              </ApiTestingProvider>
            } />

            {/* Export Routes */}
            <Route path="/export" element={
              <ExportProvider>
                <ExportPanel />
              </ExportProvider>
            } />
            <Route path="/export/:projectId" element={
              <ExportProvider>
                <ExportPanel />
              </ExportProvider>
            } />

            {/* ADO Integration Routes */}
            <Route path="/ado-integration" element={
              <AdoIntegrationProvider>
                <AdoIntegrationPanel />
              </AdoIntegrationProvider>
            } />
            <Route path="/ado-integration/:projectId" element={
              <AdoIntegrationProvider>
                <AdoIntegrationPanel />
              </AdoIntegrationProvider>
            } />

            {/* Dashboard Routes */}
            <Route path="/dashboard" element={<DashboardPanel />} />
            <Route path="/reports" element={<DashboardPanel />} />

            {/* Notifications Routes */}
            <Route path="/notifications" element={<NotificationsPanel />} />

            {/* Settings Routes */}
            <Route path="/settings" element={<SettingsPanel />} />

            {/* 404 Route */}
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </Router>
  );
};

export default App;