import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { OrgProvider } from './auth/OrgProvider';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoadingState } from './components/FeedbackStates';
import { LoginPage } from './pages/LoginPage';

// Route-level code splitting keeps the initial bundle small; the Monaco-backed
// pages (search, chat, repository detail) load on demand.
const RepositoriesPage = lazy(() =>
  import('./pages/RepositoriesPage').then((m) => ({ default: m.RepositoriesPage })),
);
const RepositoryDetailPage = lazy(() =>
  import('./pages/RepositoryDetailPage').then((m) => ({ default: m.RepositoryDetailPage })),
);
const SearchPage = lazy(() => import('./pages/SearchPage').then((m) => ({ default: m.SearchPage })));
const CodeReviewPage = lazy(() =>
  import('./pages/CodeReviewPage').then((m) => ({ default: m.CodeReviewPage })),
);
const ChatPage = lazy(() => import('./pages/ChatPage').then((m) => ({ default: m.ChatPage })));
const SettingsPage = lazy(() =>
  import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage })),
);
const NotFoundPage = lazy(() =>
  import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })),
);

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <OrgProvider>
          <Suspense fallback={<LoadingState />}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route
                element={
                  <ProtectedRoute>
                    <AppLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Navigate to="/repositories" replace />} />
                <Route path="/repositories" element={<RepositoriesPage />} />
                <Route path="/repositories/:repositoryId" element={<RepositoryDetailPage />} />
                <Route path="/search" element={<SearchPage />} />
                <Route path="/code-review" element={<CodeReviewPage />} />
                <Route path="/chat" element={<ChatPage />} />
                <Route path="/settings" element={<SettingsPage />} />
              </Route>
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </Suspense>
        </OrgProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
