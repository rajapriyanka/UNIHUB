"use client"

import { useState } from "react"
import { useNavigate } from "react-router-dom"
import StudentService from "../../Service/StudentService"
import { useStudent } from "../../Context/StudentContext"
import "./StudentLogin.css"

function StudentLogin() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setStudentInfo } = useStudent()

  

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
  
    try {
      const userData = await StudentService.login(email, password);
      console.log("Login response:", userData);
  
      if (userData && userData.jwt) {
        localStorage.setItem("token", userData.jwt);
        localStorage.setItem("role", userData.userRole);
  
        // Fix: Ensure we store the correct studentId
        const studentId = userData.studentId || userData.facultyId || null;
  
        if (!studentId) {
          throw new Error("Student ID is missing in response. Please contact support.");
        }
  
        localStorage.setItem("studentId", studentId);
  
        setStudentInfo({
          studentId,
          fullName: userData.name || "",
          email: userData.email || email,
          department: userData.department || "",
        });
  
        if (userData.userRole?.toUpperCase() === "STUDENT") {
          navigate("/student-dashboard");
        } else {
          setError("Access denied. You are not a Student.");
        }
      } else {
        setError("Invalid credentials.");
      }
    } catch (error) {
      console.error("Login error:", error);
      setError(error.message || "Login failed. Please try again.");
    } finally {
      setLoading(false);
      setTimeout(() => setError(""), 5000);
    }
  };
  

  return (
    <div className="student-login-page">
      <div className="student-login-container">
        <h2>Student Login</h2>
        {error && <p className="error-message">{error}</p>}
        <form onSubmit={handleSubmit}>
          <div className="student-login-input-group">
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
            />
          </div>
          <div className="student-login-input-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>
          <button type="submit" className="student-login-button" disabled={loading}>
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  )
}

export default StudentLogin

