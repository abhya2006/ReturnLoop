import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Link } from 'react-router-dom';

export class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  componentDidCatch(error: Error, info: ErrorInfo) { console.error('ReturnLoop screen error', error, info); }
  render() {
    if (this.state.failed) return <main className="flex min-h-screen items-center justify-center bg-jet p-6 text-ink-primary"><section className="panel max-w-md p-8 text-center"><h1 className="text-xl font-semibold">This page hit a snag</h1><p className="mt-2 text-sm text-ink-secondary">Reload the page to try again. Your account data has not been changed.</p><button className="mt-5 rounded-xl bg-cerulean px-4 py-2 text-sm font-semibold text-white" onClick={() => window.location.reload()}>Reload page</button><Link className="ml-3 text-sm text-cerulean-light" to="/">Home</Link></section></main>;
    return this.props.children;
  }
}
