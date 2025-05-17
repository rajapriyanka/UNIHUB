import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import FacultyService from "../../Service/FacultyService";
import { useFaculty } from "../../Context/FacultyContext";
import "./FacultyLogin.css";

function FacultyLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { setFacultyInfo } = useFaculty();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(""); // Clear previous errors

    try {
      const userData = await FacultyService.login(email, password);
      console.log("Login response:", userData);

      if (userData && userData.jwt) {
        // Save token and role to localStorage
        localStorage.setItem("token", userData.jwt);
        localStorage.setItem("role", userData.userRole);

        if (userData.facultyId) {
          localStorage.setItem("facultyId", userData.facultyId);
        }

        // Store faculty details in context
        setFacultyInfo({
          facultyId: userData.facultyId,
          fullName: userData.name,
          email: userData.email,
          department: userData.department,
        });

        // Navigate based on role
        if (userData.userRole?.toUpperCase() === "FACULTY") {
          navigate("/faculty-dashboard");
        } else {
          setError("Access denied. You are not a Faculty.");
        }
      } else {
        setError("Invalid credentials.");
      }
    } catch (error) {
      console.error("Login error:", error);
      setError(error.message || "Login failed. Please try again.");
      setTimeout(() => {
        setError("");
      }, 5000);
    }
  };

  return (
    <div className="faculty-login-page">
      <div className="faculty-login-container">
        <h2>Faculty Login</h2>
        {error && <p className="error-message">{error}</p>}
        <form onSubmit={handleSubmit}>
          <div className="faculty-login-input-group">
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="faculty-login-input-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="faculty-login-button">
            Login
          </button>
        </form>
      </div>
    </div>
  );
}

export default FacultyLogin;
