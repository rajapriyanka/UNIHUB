import React, { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import UserService from "../../Service/UserService"
import "./ResetPassword.css"

function ResetPassword() {
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const navigate = useNavigate()
  const location = useLocation()

  const token = new URLSearchParams(location.search).get("token")

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match")
      return
    }

    try {
      await UserService.resetPassword(token, newPassword)
      setSuccess("Password reset successful. You can now login with your new password.")
      setTimeout(() => navigate("/admin-login"), 3000)
    } catch (error) {
      console.error("Reset password error:", error)
      setError(error.message || "Failed to reset password. Please try again.")
    }
  }

  return (
    <div className="reset-password-page">
      <div className="reset-password-container">
        <h2>Reset Password</h2>
        {error && <p className="error-message">{error}</p>}
        {success && <p className="success-message">{success}</p>}
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <input
              type="password"
              placeholder="New Password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
            />
          </div>
          <div className="input-group">
            <input
              type="password"
              placeholder="Confirm New Password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="reset-password-button">
            Reset Password
          </button>
        </form>
      </div>
    </div>
  )
}

export default ResetPassword

