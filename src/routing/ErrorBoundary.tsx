import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'

type Props = { children: ReactNode }
type State = { error?: Error }

// Route-level safety net: without this, an unhandled render error or a failed
// lazy-chunk fetch (e.g. a stale cached HTML shell requesting a chunk that no
// longer exists after a redeploy) unmounts the entire React tree, taking the
// bottom nav / dashboard shell down with it. Catching at the route boundary
// keeps everything outside the failed screen intact and offers a recovery
// path, instead of a blank white page with no way back.
export class RouteErrorBoundary extends Component<Props, State> {
  state: State = {}

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Deliberate: surfaces route crashes in production logs/devtools.
    console.error('Route render error:', error, info.componentStack)
  }

  // Reset lets a transient failure (e.g. a chunk request that failed due to a
  // flaky connection) be retried without a full page reload; a hard reload is
  // offered separately for the stale-deployment case a retry can't fix.
  reset = () => this.setState({ error: undefined })

  render() {
    if (this.state.error) {
      return (
        <main className="page">
          <div className="notice">
            <AlertTriangle />
            <p>Something went wrong loading this screen. Your data is safe — try again, or reload the app.</p>
          </div>
          <div className="card-actions">
            <button className="primary-button" onClick={this.reset}>TRY AGAIN</button>
            <button onClick={() => location.reload()}>RELOAD APP</button>
          </div>
        </main>
      )
    }
    return this.props.children
  }
}
