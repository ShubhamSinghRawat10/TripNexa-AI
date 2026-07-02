import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import './GenerateTripPage.css';

const INTERESTS = ['nature', 'food', 'adventure', 'culture', 'shopping', 'nightlife', 'sightseeing', 'photography'];
const DESTINATIONS = ['Agra', 'Ahmedabad', 'Amritsar', 'Andaman', 'Bengaluru', 'Darjeeling', 'Goa', 'Hyderabad', 'Jaipur', 'Jaisalmer', 'Kochi', 'Leh', 'Manali', 'Mumbai', 'Munnar', 'Rishikesh', 'Shillong', 'Udaipur', 'Varanasi'];

const AGENT_STEPS = [
  { name: 'Crowd Agent', desc: 'Checking crowd levels...', icon: '👥' },
  { name: 'Price Agent', desc: 'Comparing hotel prices...', icon: '💰' },
  { name: 'Hotel Agent', desc: 'Finding best stays...', icon: '🏨' },
  { name: 'Budget Agent', desc: 'Allocating your budget...', icon: '📊' },
  { name: 'Gemini AI', desc: 'Building your itinerary...', icon: '🤖' },
  { name: 'Recommendation', desc: 'Computing trip score...', icon: '⭐' },
];

export default function GenerateTripPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    destination: '', budget: '', days: '', people: '', travelDate: '', interests: [],
  });
  const [error, setError] = useState('');
  const [generating, setGenerating] = useState(false);
  const [agentStep, setAgentStep] = useState(0);

  const update = (field, value) => setForm(prev => ({ ...prev, [field]: value }));

  const toggleInterest = (interest) => {
    setForm(prev => ({
      ...prev,
      interests: prev.interests.includes(interest)
        ? prev.interests.filter(i => i !== interest)
        : [...prev.interests, interest],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setGenerating(true);
    setAgentStep(0);

    // Simulate agent progress while API runs
    const interval = setInterval(() => {
      setAgentStep(prev => Math.min(prev + 1, AGENT_STEPS.length - 1));
    }, 2000);

    try {
      const { data } = await api.post('/trip/generate', {
        destination: form.destination,
        budget: Number(form.budget),
        days: Number(form.days),
        people: Number(form.people),
        travelDate: form.travelDate,
        interests: form.interests,
      });
      clearInterval(interval);
      navigate('/result', { state: { result: data } });
    } catch (err) {
      clearInterval(interval);
      setError(err.response?.data?.message || 'Failed to generate trip. Please try again.');
      setGenerating(false);
    }
  };

  if (generating) {
    return (
      <main className="generate-page container" id="generating-state">
        <div className="generating-card glass-card animate-fade-in">
          <h2>🤖 AI Agents Working...</h2>
          <p className="gen-subtitle">Our multi-agent system is planning your perfect trip</p>
          <div className="agent-steps">
            {AGENT_STEPS.map((step, i) => (
              <div className={`agent-step ${i < agentStep ? 'done' : i === agentStep ? 'active' : ''}`} key={i}>
                <span className="agent-icon">{step.icon}</span>
                <div className="agent-info">
                  <span className="agent-name">{step.name}</span>
                  <span className="agent-desc">{step.desc}</span>
                </div>
                {i < agentStep && <span className="agent-check">✓</span>}
                {i === agentStep && <div className="spinner" style={{width: 20, height: 20}}></div>}
              </div>
            ))}
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="generate-page container" id="generate-trip-page">
      <div className="generate-card glass-card animate-fade-in">
        <div className="gen-header">
          <h1>Plan Your Trip ✈️</h1>
          <p>Fill in your travel details and let our AI agents handle the rest</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="trip-form">
          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="gen-destination">Destination</label>
              <select id="gen-destination" className="form-input" value={form.destination}
                onChange={e => update('destination', e.target.value)} required>
                <option value="">Select destination</option>
                {DESTINATIONS.map(d => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="gen-budget">Total Budget (₹)</label>
              <input id="gen-budget" type="number" className="form-input" placeholder="e.g. 15000"
                value={form.budget} onChange={e => update('budget', e.target.value)}
                min="1" required />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label" htmlFor="gen-days">Days</label>
              <input id="gen-days" type="number" className="form-input" placeholder="e.g. 3"
                value={form.days} onChange={e => update('days', e.target.value)}
                min="1" max="30" required />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="gen-people">People</label>
              <input id="gen-people" type="number" className="form-input" placeholder="e.g. 2"
                value={form.people} onChange={e => update('people', e.target.value)}
                min="1" max="50" required />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="gen-date">Travel Date</label>
              <input id="gen-date" type="date" className="form-input"
                value={form.travelDate} onChange={e => update('travelDate', e.target.value)} required />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Interests</label>
            <div className="interest-grid">
              {INTERESTS.map(interest => (
                <button type="button" key={interest}
                  className={`interest-chip ${form.interests.includes(interest) ? 'selected' : ''}`}
                  onClick={() => toggleInterest(interest)}>
                  {interest}
                </button>
              ))}
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-lg auth-submit" id="gen-submit"
            disabled={form.interests.length === 0}>
            🚀 Generate Itinerary
          </button>
        </form>
      </div>
    </main>
  );
}
