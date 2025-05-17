"use client"

import { useState } from "react"
import axios from "axios"
import "./AdminProfile.css"
import AdminNavbar from "../Land/AdminNavbar" // Assuming this component exists

const AdminProfile = () => {
  // Form states
  const [email, setEmail] = useState("")

  // Password reset states
  const [showPasswordSection, setShowPasswordSection] = useState(true)
  const [passwordResetStep, setPasswordResetStep] = useState("request") // request, verify, success
  const [otp, setOtp] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [successMessage, setSuccessMessage] = useState("")
  const [errorMessage, setErrorMessage] = useState("")
  const [username, setUsername] = useState("")
  const [newUsername, setNewUsername] = useState("")
  const [showUsernameSection, setShowUsernameSection] = useState(false)

  // Password validation states
  const [passwordRequirements, setPasswordRequirements] = useState({
    length: false,
    lowercase: false,
    uppercase: false,
    number: false,
    special: false,
  })
  const [passwordStrength, setPasswordStrength] = useState(0)

  const requestPasswordReset = async (e) => {
    e.preventDefault()
    setSuccessMessage("")
    setErrorMessage("")

    if (!email) {
      setErrorMessage("Please enter your email address.")
      return
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      setErrorMessage("Please enter a valid email address.")
      return
    }

    try {
      await axios.post("http://localhost:8080/api/admin/password/reset-request", {
        email: email,
      })

      setSuccessMessage("If the email exists, a password reset OTP has been sent")
      setPasswordResetStep("verify")
    } catch (error) {
      console.error("Error requesting password reset:", error)
      setErrorMessage("Error requesting password reset. Please try again.")
    }
  }

  // Change the changeUsername function to match the expected API contract
  const changeUsername = async (e) => {
    e.preventDefault()
    setSuccessMessage("")
    setErrorMessage("")

    if (!email) {
      setErrorMessage("Please enter your email address.")
      return
    }

    if (!newUsername) {
      setErrorMessage("New username is required.")
      return
    }

    try {
      await axios.post("http://localhost:8080/api/admin/password/change-email", {
        email: email,
        newEmail: newUsername, // Changed from newUsername to newEmail to match the API contract
      })

      setSuccessMessage("Username has been updated successfully")
      setUsername(newUsername)
      setNewUsername("")
    } catch (error) {
      console.error("Error changing username:", error)
      setErrorMessage(error.response?.data?.message || "Error changing username. Please try again.")
    }
  }

  // Add a toggle function to switch between password and username sections
  const toggleSection = (section) => {
    if (section === "password") {
      setShowPasswordSection(true)
      setShowUsernameSection(false)
    } else {
      setShowPasswordSection(false)
      setShowUsernameSection(true)
    }
    setSuccessMessage("")
    setErrorMessage("")
  }

  const resendOtp = async () => {
    setSuccessMessage("")
    setErrorMessage("")

    try {
      await axios.post("http://localhost:8080/api/admin/password/resend-otp", {
        email: email,
      })

      setSuccessMessage("If the email exists, a new OTP has been sent")
    } catch (error) {
      console.error("Error resending OTP:", error)
      setErrorMessage("Error resending OTP. Please try again.")
    }
  }

  const validatePassword = (password) => {
    const requirements = {
      length: password.length >= 8,
      lowercase: /[a-z]/.test(password),
      uppercase: /[A-Z]/.test(password),
      number: /[0-9]/.test(password),
      special: /[^A-Za-z0-9]/.test(password),
    }

    setPasswordRequirements(requirements)

    // Calculate password strength (0-5)
    const strength = Object.values(requirements).filter(Boolean).length
    setPasswordStrength(strength)

    return Object.values(requirements).every(Boolean)
  }

  const verifyOtpAndResetPassword = async (e) => {
    e.preventDefault()
    setSuccessMessage("")
    setErrorMessage("")

    if (!otp) {
      setErrorMessage("Please enter the OTP received in your email.")
      return
    }

    if (!newPassword) {
      setErrorMessage("New password is required.")
      return
    }

    // Check if password meets all requirements
    if (!validatePassword(newPassword)) {
      setErrorMessage("Please ensure your password meets all the requirements.")
      return
    }

    try {
      await axios.post("http://localhost:8080/api/admin/password/reset-verify", {
        email: email,
        otp: otp,
        newPassword: newPassword,
      })

      setSuccessMessage("Password has been reset successfully")
      setPasswordResetStep("success")

      // Reset form fields
      setOtp("")
      setNewPassword("")
      setPasswordRequirements({
        length: false,
        lowercase: false,
        uppercase: false,
        number: false,
        special: false,
      })
      setPasswordStrength(0)
    } catch (error) {
      console.error("Error verifying OTP and resetting password:", error)
      setErrorMessage(error.response?.data?.message || "Error verifying OTP and resetting password. Please try again.")
    }
  }

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword)
  }

  const resetForm = () => {
    setPasswordResetStep("request")
    setEmail("")
    setOtp("")
    setNewPassword("")
    setSuccessMessage("")
    setErrorMessage("")
    setPasswordRequirements({
      length: false,
      lowercase: false,
      uppercase: false,
      number: false,
      special: false,
    })
    setPasswordStrength(0)
  }

  return (
    <div className="admin-profile-page">
      <AdminNavbar />
      <div className="admin-profile-container">
        <div className="admin-profile-main-content">
          <div className="profile-card">
            <h2>Admin Profile Management</h2>

            <div className="section-toggle">
              <button
                className={`toggle-button ${showPasswordSection ? "active" : ""}`}
                onClick={() => toggleSection("password")}
              >
                Reset Password
              </button>
              <button
                className={`toggle-button ${showUsernameSection ? "active" : ""}`}
                onClick={() => toggleSection("username")}
              >
                Change Username
              </button>
            </div>

            <div className="password-section" style={{ display: showPasswordSection ? "block" : "none" }}>
              {passwordResetStep === "request" && (
                <form onSubmit={requestPasswordReset}>
                  <div className="input-group">
                    <label htmlFor="email">Email Address</label>
                    <input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="Enter your admin email"
                      required
                    />
                  </div>
                  <p className="info-text">
                    Enter your admin email address. We'll send you a one-time password (OTP) to reset your password.
                  </p>
                  <button type="submit" className="request-otp-button">
                    Request OTP
                  </button>
                </form>
              )}

              {passwordResetStep === "verify" && (
                <form onSubmit={verifyOtpAndResetPassword}>
                  <div className="input-group">
                    <label htmlFor="email">Email Address</label>
                    <input id="email" type="email" value={email} disabled className="disabled-input" />
                  </div>

                  <div className="input-group">
                    <label htmlFor="otp">OTP</label>
                    <input
                      id="otp"
                      type="text"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value)}
                      placeholder="Enter OTP from your email"
                    />
                  </div>

                  <div className="input-group password-input-container">
                    <label htmlFor="new-password">New Password</label>
                    <div className="password-field">
                      <input
                        id="new-password"
                        type={showPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={(e) => {
                          setNewPassword(e.target.value)
                          validatePassword(e.target.value)
                        }}
                        placeholder="Enter new password"
                        className="password-input"
                      />
                      <button
                        type="button"
                        className="toggle-password-visibility"
                        onClick={togglePasswordVisibility}
                        aria-label={showPassword ? "Hide password" : "Show password"}
                      >
                        {showPassword ? "Hide" : "Show"}
                      </button>
                    </div>

                    {newPassword.length > 0 && (
                      <div className="password-requirements">
                        <div className="password-strength-container">
                          <div className="password-strength-meter">
                            <div
                              className="password-strength-bar"
                              style={{
                                width: `${(passwordStrength / 5) * 100}%`,
                                backgroundColor:
                                  passwordStrength < 2 ? "#ff4d4f" : passwordStrength < 4 ? "#faad14" : "#52c41a",
                              }}
                            ></div>
                          </div>
                          <p className="password-strength-text">
                            {passwordStrength < 2 ? "Weak" : passwordStrength < 4 ? "Medium" : "Strong"}
                          </p>
                        </div>
                        <ul className="requirements-list">
                          <li className={passwordRequirements.length ? "met" : "not-met"}>
                            <span className="requirement-icon"></span>
                            <span className="requirement-text">At least 8 characters</span>
                          </li>
                          <li className={passwordRequirements.lowercase ? "met" : "not-met"}>
                            <span className="requirement-icon"></span>
                            <span className="requirement-text">At least one lowercase letter (a-z)</span>
                          </li>
                          <li className={passwordRequirements.uppercase ? "met" : "not-met"}>
                            <span className="requirement-icon"></span>
                            <span className="requirement-text">At least one uppercase letter (A-Z)</span>
                          </li>
                          <li className={passwordRequirements.number ? "met" : "not-met"}>
                            <span className="requirement-icon"></span>
                            <span className="requirement-text">At least one number (0-9)</span>
                          </li>
                          <li className={passwordRequirements.special ? "met" : "not-met"}>
                            <span className="requirement-icon"></span>
                            <span className="requirement-text">At least one special character (e.g., !@#$%^&*)</span>
                          </li>
                        </ul>
                      </div>
                    )}
                  </div>

                  <div className="button-group">
                    <button type="submit" className="change-password-button">
                      Reset Password
                    </button>
                    <button type="button" className="resend-otp-button" onClick={resendOtp}>
                      Resend OTP
                    </button>
                    <button type="button" className="back-button" onClick={() => setPasswordResetStep("request")}>
                      Back
                    </button>
                  </div>
                </form>
              )}

              {passwordResetStep === "success" && (
                <div className="success-step">
                  <div className="success-icon">✓</div>
                  <h3>Password Reset Successful</h3>
                  <p>Your password has been reset successfully.</p>
                  <p>You can now use your new password to log in.</p>
                  <button className="login-button" onClick={resetForm}>
                    Reset Another Password
                  </button>
                </div>
              )}
            </div>

            <div className="username-section" style={{ display: showUsernameSection ? "block" : "none" }}>
              <form onSubmit={changeUsername}>
                <div className="input-group">
                  <label htmlFor="email-username">Email Address</label>
                  <input
                    id="email-username"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Enter your admin email"
                    required
                  />
                </div>

                <div className="input-group">
                  <label htmlFor="new-username">New Username</label>
                  <input
                    id="new-username"
                    type="text"
                    value={newUsername}
                    onChange={(e) => setNewUsername(e.target.value)}
                    placeholder="Enter new username"
                    required
                  />
                </div>

                <p className="info-text">Enter your admin email address and the new username you want to use.</p>

                <button type="submit" className="change-username-button">
                  Change Username
                </button>
              </form>
            </div>

            {successMessage && <p className="success-message">{successMessage}</p>}
            {errorMessage && <p className="error-message">{errorMessage}</p>}
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminProfile

