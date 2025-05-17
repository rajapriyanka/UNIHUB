import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import UserService from "../../Service/UserService";
import './AdminLogin.css';

function AdminLogin() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const userData = await UserService.login(email, password);
      console.log(userData);

      if (userData && userData.jwt) {
        // Save token and role to localStorage
        localStorage.setItem('token', userData.jwt);
        localStorage.setItem('role', userData.userRole);

        // Navigate based on role
        if (userData.userRole === 'ADMIN') {
          navigate('/admin-dashboard');
        } else {
          setError('Access denied. You are not an admin.');
        }
      } else {
        setError('Invalid credentials.');
      }
    } catch (error) {
      console.error('Login error:', error);
      setError(error.message || 'Login failed. Please try again.');
      setTimeout(() => {
        setError('');
      }, 5000);
    }
  };

  return (
    <div className="admin-login-page">
      <div className="admin-login-container">
        <h2>Admin Login</h2>
        {error && <p className="error-message">{error}</p>}
        <form onSubmit={handleSubmit}>
          <div className="admin-login-input-group">
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="admin-login-input-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="admin-login-button">Login</button>
        </form>
      </div>
    </div>
  );
}

export default AdminLogin;
