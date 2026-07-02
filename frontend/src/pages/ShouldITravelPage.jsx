import { useState } from 'react';
import api from '../api/client';
import './ShouldITravelPage.css';

const DESTINATIONS = ['Agra', 'Ahmedabad', 'Amritsar', 'Andaman', 'Bengaluru', 'Darjeeling', 'Goa', 'Hyderabad', 'Jaipur', 'Jaisalmer', 'Kochi', 'Leh', 'Manali', 'Mumbai', 'Munnar', 'Rishikesh', 'Shillong', 'Udaipur', 'Varanasi'];

export default function ShouldITravelPage() {
  const [destination, setDestination] = useState('');
  const [budget, setBudget] = useState('');
  const [date, setDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const handleCheck = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    setResult(null);
    try {
      const { data } = await api.get('/should-i-travel', {
        params: { destination, budget: Number(budget), date },
      });
      setResult(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Check failed. Try again.');
    } finally {
      setLoading(false);
    }
  };

  const scoreColor = (result?.recommendation?.tripScore ?? 0) >= 70 ? 'var(--success)' :
                     (result?.recommendation?.tripScore ?? 0) >= 40 ? 'var(--warning)' : 'var(--danger)';

  return (
    <main className="sit-page container" id="should-i-travel-page">
      <div className="sit-header animate-fade-in">
        <span className="sit-emoji">🤔</span>
        <h1>Should I <span className="text-gradient">Travel Now?</span></h1>
        <p>Get instant crowd, price, and recommendation insights — no login required</p>
      </div>

      <div className="sit-layout">
        <form onSubmit={handleCheck} className="sit-form glass-card animate-slide-up" id="sit-form">
          <div className="form-group">
            <label className="form-label" htmlFor="sit-destination">Where to?</label>
            <select id="sit-destination" className="form-input" value={destination}
              onChange={e => setDestination(e.target.value)} required>
              <option value="">Select destination</option>
              {DESTINATIONS.map(d => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="sit-budget">Budget (₹)</label>
            <input id="sit-budget" type="number" className="form-input" placeholder="e.g. 15000"
              value={budget} onChange={e => setBudget(e.target.value)} min="1" required />
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="sit-date">Travel Date</label>
            <input id="sit-date" type="date" className="form-input"
              value={date} onChange={e => setDate(e.target.value)} required />
          </div>
          <button type="submit" className="btn btn-primary btn-lg" disabled={loading} id="sit-submit">
            {loading ? '⏳ Analyzing...' : '🔍 Check Now'}
          </button>
        </form>

        {error && <div className="auth-error sit-error">{error}</div>}

        {result && (
          <div className="sit-results animate-fade-in">
            {/* Score */}
            {result.recommendation && (
              <div className="sit-score-card glass-card">
                <div className="score-circle" style={{ borderColor: scoreColor, color: scoreColor, width: 100, height: 100, fontSize: '2rem' }}>
                  {result.recommendation.tripScore}
                </div>
                <div className="sit-score-info">
                  <h3>Trip Score</h3>
                  <p>{result.recommendation.verdict}</p>
                </div>
              </div>
            )}

            {/* Crowd & Price Cards */}
            <div className="sit-detail-grid">
              {result.crowd && (
                <div className="sit-detail glass-card" id="sit-crowd-card">
                  <div className="sit-detail-header">
                    <span className="sit-detail-icon">👥</span>
                    <h3>Crowd Level</h3>
                  </div>
                  <span className={`badge badge-${result.crowd.level === 'HIGH' ? 'danger' : result.crowd.level === 'MEDIUM' ? 'warning' : 'success'}`}
                    style={{fontSize: '0.9rem', padding: '0.4rem 1rem'}}>
                    {result.crowd.level}
                  </span>
                  <p className="sit-detail-reason">{result.crowd.reason}</p>
                </div>
              )}

              {result.price && (
                <div className="sit-detail glass-card" id="sit-price-card">
                  <div className="sit-detail-header">
                    <span className="sit-detail-icon">💰</span>
                    <h3>Hotel Pricing</h3>
                  </div>
                  <div className="price-comparison">
                    <div className="price-item">
                      <span className="price-label">Current</span>
                      <span className="price-value">₹{result.price.currentPrice.toLocaleString()}</span>
                    </div>
                    <div className="price-arrow">{result.price.surgePercent > 0 ? '📈' : '📉'}</div>
                    <div className="price-item">
                      <span className="price-label">Baseline</span>
                      <span className="price-value">₹{result.price.baselinePrice.toLocaleString()}</span>
                    </div>
                  </div>
                  {result.price.surgePercent > 0 && (
                    <span className="badge badge-warning" style={{marginTop: '0.5rem'}}>
                      +{result.price.surgePercent}% surge
                    </span>
                  )}
                </div>
              )}
            </div>

            {/* Savings Suggestion */}
            {result.recommendation?.estimatedSavings > 0 && (
              <div className="sit-savings glass-card" id="sit-savings-card">
                <div className="savings-big">
                  <span className="savings-label">You could save</span>
                  <span className="savings-value text-gradient">
                    ₹{result.recommendation.estimatedSavings.toLocaleString()}
                  </span>
                </div>
                <p>by travelling on <strong>{result.recommendation.bestTravelDate}</strong> instead</p>
              </div>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
