import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './LandingPage.css';

export default function LandingPage() {
  const { isAuthenticated } = useAuth();

  return (
    <main className="landing" id="landing-page">
      {/* Animated background orbs */}
      <div className="bg-orbs">
        <div className="orb orb-1"></div>
        <div className="orb orb-2"></div>
        <div className="orb orb-3"></div>
      </div>

      <section className="hero container">
        <div className="hero-content animate-fade-in">
          <div className="hero-badge badge badge-accent">✨ AI-Powered Multi-Agent System</div>
          <h1 className="hero-title">
            Plan Smarter Trips with <span className="text-gradient">TripNexa AI</span>
          </h1>
          <p className="hero-subtitle">
            Our multi-agent system analyzes crowd levels, hotel prices, weather, and your budget
            to generate the perfect travel itinerary — and tells you if <em>now</em> is even the right time to go.
          </p>
          <div className="hero-cta">
            {isAuthenticated ? (
              <Link to="/generate" className="btn btn-primary btn-lg" id="cta-generate">
                🚀 Plan My Trip
              </Link>
            ) : (
              <Link to="/signup" className="btn btn-primary btn-lg" id="cta-signup">
                🚀 Get Started Free
              </Link>
            )}
            <Link to="/should-i-travel" className="btn btn-secondary btn-lg" id="cta-should-travel">
              🤔 Should I Travel Now?
            </Link>
          </div>
        </div>

        {/* Feature cards */}
        <div className="features" id="features-section">
          {[
            { icon: '👥', title: 'Crowd Agent', desc: 'Detects peak dates, festivals & weekends' },
            { icon: '💰', title: 'Price Agent', desc: 'Compares hotel prices vs baseline' },
            { icon: '🏨', title: 'Hotel Agent', desc: 'Finds best stays within your budget' },
            { icon: '📊', title: 'Budget Agent', desc: 'Smart allocation across categories' },
            { icon: '🤖', title: 'Gemini AI', desc: 'Generates personalized day-wise plans' },
            { icon: '⭐', title: 'Trip Score', desc: '"Should I travel now?" with savings estimate' },
          ].map((f, i) => (
            <div className="feature-card glass-card animate-slide-up" key={i}
                 style={{ animationDelay: `${i * 0.1}s` }}>
              <span className="feature-icon">{f.icon}</span>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="how-it-works container">
        <h2 className="text-center">How It Works</h2>
        <div className="steps">
          {[
            { step: '01', title: 'Enter Details', desc: 'Destination, budget, dates, interests' },
            { step: '02', title: 'Agents Analyze', desc: '6 AI agents work in parallel' },
            { step: '03', title: 'Get Your Plan', desc: 'Itinerary, hotels, budget & score' },
          ].map((s, i) => (
            <div className="step-card" key={i}>
              <div className="step-number text-gradient">{s.step}</div>
              <h3>{s.title}</h3>
              <p>{s.desc}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
