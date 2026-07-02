import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar" id="main-nav">
      <div className="navbar-inner container">
        <Link to="/" className="navbar-brand">
          <span className="brand-icon">✈️</span>
          <span className="brand-text">Trip<span className="text-gradient">Nexa</span></span>
        </Link>

        <div className="navbar-links">
          <Link to="/should-i-travel" className="nav-link" id="nav-should-travel">
            Should I Travel?
          </Link>
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" className="nav-link" id="nav-dashboard">Dashboard</Link>
              <Link to="/generate" className="btn btn-primary btn-sm" id="nav-generate">
                + Plan Trip
              </Link>
              <button onClick={handleLogout} className="btn btn-ghost btn-sm" id="nav-logout">
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="nav-link" id="nav-login">Login</Link>
              <Link to="/signup" className="btn btn-primary btn-sm" id="nav-signup">Sign Up</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
