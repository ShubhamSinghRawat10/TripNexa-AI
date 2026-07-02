import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import './DashboardPage.css';

export default function DashboardPage() {
  const { user } = useAuth();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/trip/history')
      .then(res => setTrips(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="dashboard container" id="dashboard-page">
      <div className="dashboard-header animate-fade-in">
        <div>
          <h1>Welcome, <span className="text-gradient">{user?.name || 'Traveler'}</span> 👋</h1>
          <p className="text-secondary">Ready to plan your next adventure?</p>
        </div>
        <Link to="/generate" className="btn btn-primary" id="dashboard-new-trip">
          + Plan New Trip
        </Link>
      </div>

      <div className="dashboard-grid">
        <Link to="/generate" className="quick-action glass-card" id="qa-generate">
          <span className="qa-icon">🗺️</span>
          <h3>Generate Trip</h3>
          <p>AI-powered itinerary</p>
        </Link>
        <Link to="/should-i-travel" className="quick-action glass-card" id="qa-should-travel">
          <span className="qa-icon">🤔</span>
          <h3>Should I Travel?</h3>
          <p>Check crowd & prices</p>
        </Link>
      </div>

      <section className="trip-history">
        <h2>Your Trips</h2>
        {loading ? (
          <div className="loading-state"><div className="spinner"></div></div>
        ) : trips.length === 0 ? (
          <div className="empty-state glass-card">
            <span className="empty-icon">✈️</span>
            <h3>No trips yet</h3>
            <p>Create your first AI-powered travel plan!</p>
            <Link to="/generate" className="btn btn-primary">Plan a Trip</Link>
          </div>
        ) : (
          <div className="trips-grid">
            {trips.map(trip => (
              <div className="trip-card glass-card" key={trip.id} id={`trip-${trip.id}`}>
                <div className="trip-card-header">
                  <h3>📍 {trip.destination}</h3>
                  <span className={`badge badge-${trip.status === 'GENERATED' ? 'success' : 'accent'}`}>
                    {trip.status}
                  </span>
                </div>
                <div className="trip-card-details">
                  <div className="trip-detail">
                    <span className="detail-label">Budget</span>
                    <span className="detail-value">₹{trip.budget.toLocaleString()}</span>
                  </div>
                  <div className="trip-detail">
                    <span className="detail-label">Duration</span>
                    <span className="detail-value">{trip.days} days</span>
                  </div>
                  <div className="trip-detail">
                    <span className="detail-label">People</span>
                    <span className="detail-value">{trip.people}</span>
                  </div>
                  <div className="trip-detail">
                    <span className="detail-label">Date</span>
                    <span className="detail-value">{trip.travelDate}</span>
                  </div>
                </div>
                <div className="trip-card-tags">
                  {trip.interests.map(i => (
                    <span className="badge badge-accent" key={i}>{i}</span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
