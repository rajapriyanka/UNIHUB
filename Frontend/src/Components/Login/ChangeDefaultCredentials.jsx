import React, { useState } from "react"
import { useNavigate } from "react-router-dom"
import UserService from "../../Service/UserService"
import "./ChangeDefaultCredentials.css"

function ChangeDefaultCredentials() {
  const [newEmail, setNewEmail] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError("")
    setSuccess("")

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match")
      return
    }

    try {
      await UserService.changeDefaultCredentials(newEmail, newPassword)
      setSuccess("Default admin credentials changed successfully")
      setTimeout(() => navigate("/admin-dashboard"), 3000)
    } catch (error) {
      console.error("Change default credentials error:", error)
      setError(error.message || "Failed to change default credentials. Please try again.")
    }
  }

  return (
    <div className="change-default-credentials-page">
      <div className="change-default-credentials-container">
        <h2>Change Default Admin Credentials</h2>
        {error && <p className="error-message">{error}</p>}
        {success && <p className="success-message">{success}</p>}
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <input
              type="email"
              placeholder="New Email"
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              required
            />
          </div>
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
          <button type="submit" className="change-credentials-button">
            Change Credentials
          </button>
        </form>
      </div>
    </div>
  )
}

export default ChangeDefaultCredentials

