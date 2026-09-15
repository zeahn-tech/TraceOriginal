import { Brand, Page } from '../../shared/ui'
import { logo } from '../../shared/assets'

export function AboutPage(){return <Page title="About TraceNet"><section className="about"><img src={logo} alt="TraceNet Liberia"/><Brand/><h2>Securing Communities Together</h2><p>TraceNet Liberia connects citizens, law enforcement and emergency response organizations through secure reports, real-time alerts, verified notices and coordinated safety information.</p><h2>Features</h2><ul><li>Incident reports and anonymous tips</li><li>Real-time safety alerts and SOS</li><li>Verified wanted-person notices</li><li>Emergency response directory and analytics</li></ul><p className="muted">Powered by Zeahn's Tech</p></section></Page>}
