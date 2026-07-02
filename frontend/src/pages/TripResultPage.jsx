import { useLocation, Link, Navigate } from 'react-router-dom';
import './TripResultPage.css';

export default function TripResultPage() {
  const { state } = useLocation();
  if (!state?.result) return <Navigate to="/generate" replace />;

  const r = state.result;
  const rec = r.recommendation;
  const budget = r.budget;

  const scoreColor = (rec?.tripScore ?? 0) >= 70 ? 'var(--success)' :
                     (rec?.tripScore ?? 0) >= 40 ? 'var(--warning)' : 'var(--danger)';

  return (
    <main className="result-page container" id="trip-result-page">
      {/* Hero Header */}
      <header className="result-hero animate-fade-in">
        <div className="result-hero-content">
          <span className="result-destination-badge badge badge-accent">📍 {r.destination}</span>
          <h1>Your Trip Plan is Ready!</h1>
          <div className="result-meta">
            <span>📅 {r.travelDate}</span>
            <span>⏱ {r.days} days</span>
            <span>👥 {r.people} people</span>
          </div>
        </div>
        {rec && (
          <div className="score-card glass-card">
            <div className="score-circle" style={{ borderColor: scoreColor, color: scoreColor }}>
              {rec.tripScore}
            </div>
            <span className="score-label">Trip Score</span>
          </div>
        )}
      </header>

      {/* Recommendation Verdict */}
      {rec && (
        <section className="verdict-section glass-card animate-slide-up" id="verdict-section">
          <div className="verdict-header">
            <h2>🤔 Should You Travel Now?</h2>
            <span className={`badge badge-${rec.crowdLevel === 'HIGH' ? 'danger' : rec.crowdLevel === 'MEDIUM' ? 'warning' : 'success'}`}>
              {rec.crowdLevel} Crowd
            </span>
          </div>
          <p className="verdict-text">{rec.verdict}</p>
          {rec.estimatedSavings > 0 && (
            <div className="savings-card">
              <div className="savings-amount">
                <span className="savings-label">Potential Savings</span>
                <span className="savings-value text-gradient">₹{rec.estimatedSavings.toLocaleString()}</span>
              </div>
              <div className="savings-info">
                <span>Best date: <strong>{rec.bestTravelDate}</strong></span>
              </div>
            </div>
          )}
        </section>
      )}

      <div className="result-grid">
        {/* Budget Breakdown */}
        {budget && (
          <section className="budget-section glass-card animate-slide-up" id="budget-section" style={{animationDelay: '0.1s'}}>
            <h2>💰 Budget Breakdown</h2>
            <div className="budget-bars">
              {[
                { label: 'Hotel', value: budget.hotel, color: '#6366f1' },
                { label: 'Food', value: budget.food, color: '#10b981' },
                { label: 'Transport', value: budget.transport, color: '#f59e0b' },
                { label: 'Activities', value: budget.activities, color: '#a855f7' },
              ].map(item => {
                const pct = budget.total > 0 ? (item.value / budget.total * 100) : 0;
                return (
                  <div className="budget-bar-row" key={item.label}>
                    <div className="bar-label">
                      <span>{item.label}</span>
                      <span className="bar-amount">₹{item.value.toLocaleString()}</span>
                    </div>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${pct}%`, background: item.color }}></div>
                    </div>
                  </div>
                );
              })}
            </div>
            <div className="budget-total">
              <span>Total</span>
              <span className="budget-total-value">₹{budget.total.toLocaleString()}</span>
            </div>
            <p className="budget-note">{budget.note}</p>
          </section>
        )}

        {/* Hotel Recommendations */}
        {r.hotels && (
          <section className="hotels-section glass-card animate-slide-up" id="hotels-section" style={{animationDelay: '0.2s'}}>
            <h2>🏨 Hotel Options</h2>
            <div className="hotel-list">
              {r.hotels.options.map((hotel, i) => (
                <div className={`hotel-card ${r.hotels.recommended?.name === hotel.name ? 'recommended' : ''}`} key={i}>
                  {r.hotels.recommended?.name === hotel.name && (
                    <span className="badge badge-success rec-badge">⭐ Recommended</span>
                  )}
                  <h3>{hotel.name}</h3>
                  <div className="hotel-details">
                    <span className="hotel-price">₹{hotel.pricePerNight.toLocaleString()}<small>/night</small></span>
                    <span className="hotel-rating">{'⭐'.repeat(Math.round(hotel.rating))} {hotel.rating}</span>
                    <span className="badge badge-accent">{hotel.category}</span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>

      {/* Itinerary */}
      {r.itinerary && r.itinerary.length > 0 && (
        <section className="itinerary-section animate-slide-up" id="itinerary-section" style={{animationDelay: '0.3s'}}>
          <h2>📋 Day-wise Itinerary</h2>
          <div className="itinerary-timeline">
            {r.itinerary.map((day) => (
              <div className="day-card glass-card" key={day.day}>
                <div className="day-header">
                  <span className="day-number text-gradient">Day {day.day}</span>
                </div>
                <div className="activity-list">
                  {day.activities.map((act, j) => (
                    <div className="activity-item" key={j}>
                      <div className="activity-time">
                        <span>{act.startTime}</span>
                        <div className="time-line"></div>
                        <span>{act.endTime}</span>
                      </div>
                      <div className="activity-content">
                        <h4>{act.name}</h4>
                        <div className="activity-meta">
                          <span className="badge badge-accent">{act.category}</span>
                          {act.estimatedCost > 0 && (
                            <span className="activity-cost">₹{act.estimatedCost.toLocaleString()}</span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Actions */}
      <div className="result-actions animate-fade-in">
        <Link to="/generate" className="btn btn-primary btn-lg" id="result-new-trip">
          🔄 Plan Another Trip
        </Link>
        <Link to="/dashboard" className="btn btn-secondary btn-lg" id="result-dashboard">
          ← Back to Dashboard
        </Link>
      </div>
    </main>
  );
}
