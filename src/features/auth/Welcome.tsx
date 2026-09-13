import { Link } from 'react-router-dom'
import { Brand } from '../../shared/ui'
import { logo } from '../../shared/assets'

export function Welcome(){return <main className="auth-page"><img className="auth-logo" src={logo}/><p>Welcome to</p><Brand/><p className="auth-copy">TraceNet is a public safety platform where law enforcement and citizens collaborate to find missing persons, locate wanted individuals, and improve community safety with real-time alerts and encrypted communications.</p><div className="auth-actions"><Link className="outline-button" to="/sign-in">SIGN IN</Link><Link className="outline-button" to="/sign-up">CREATE ACCOUNT</Link><Link className="auth-link" to="/public">Continue as Public Viewer</Link></div></main>}
