"use client"

import { useState, useEffect } from "react"
import axios from "axios"
import { useFaculty } from "../../Context/FacultyContext"
import "./FacultyProfile.css"
import FacultyNavbar from "../Land/FacultyNavbar"

const FacultyProfile = () => {
  const { facultyData, setFacultyInfo } = useFaculty()
  const [isLoading, setIsLoading] = useState(true) // Prevent premature redirect
  const [facultyId, setFacultyId] = useState("")
  const [userId, setUserId] = useState("")
  const [fullName, setFullName] = useState("")
  const [email, setEmail] = useState("")
  const [department, setDepartment] = useState("")
  const [designation, setDesignation] = useState("")
  const [phone, setPhone] = useState("")

  // Password change states
  const [showPasswordSection, setShowPasswordSection] = useState(false)
  const [otp, setOtp] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [otpSent, setOtpSent] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const [successMessage, setSuccessMessage] = useState("")
  const [errorMessage, setErrorMessage] = useState("")
  const [isEditable, setIsEditable] = useState(false)
  const [isModalOpen, setIsModalOpen] = useState(true)

  // Validation states
  const [nameError, setNameError] = useState("")
  const [phoneError, setPhoneError] = useState("")

  // Password validation state variables
  const [passwordRequirements, setPasswordRequirements] = useState({
    length: false,
    lowercase: false,
    uppercase: false,
    number: false,
    special: false,
  })
  const [passwordStrength, setPasswordStrength] = useState(0)
  const designations = ["Professor", "Assistant Professor", "Associate Professor"]

  useEffect(() => {
    if (facultyData === null) {
      const storedFaculty = localStorage.getItem("facultyData")
      if (storedFaculty) {
        setFacultyInfo(JSON.parse(storedFaculty))
      } else {
        window.location.href = "/"
      }
    } else {
      fetchFacultyDetails()
    }
    setIsLoading(false)
  }, [facultyData])

  const fetchFacultyDetails = async () => {
    try {
      const response = await axios.get(`http://localhost:8080/api/faculty/profile`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      })

      if (response.data) {
        setUserId(response.data.userId)
        setFacultyId(response.data.facultyId)
        setFullName(response.data.name)
        setEmail(response.data.email)
        setDepartment(response.data.department)
        setDesignation(response.data.designation)
        setPhone(response.data.mobileNo)
      }
    } catch (error) {
      setErrorMessage("Error fetching faculty details. Please try again.")
    }
  }

  // Validate name - at least 4 characters and only alphabets
  const validateName = (name) => {
    if (!name) return "Full Name is required."
    if (name.length < 4) return "Full Name must be at least 4 characters."
    if (!/^[a-zA-Z\s]+$/.test(name)) return "Full Name should contain only alphabets."
    return ""
  }

  // Validate phone - exactly 10 digits, starting digit not 0-5
  const validatePhone = (phone) => {
    if (!phone) return "Phone number is required."
    if (!/^\d{10}$/.test(phone)) return "Phone number must be exactly 10 digits."
    if (/^[0-5]/.test(phone)) return "Phone number should not start with digits 0-5."
    return ""
  }

  // Handle name change with validation
  const handleNameChange = (e) => {
    const value = e.target.value
    setFullName(value)
    if (isEditable) {
      setNameError(validateName(value))
    }
  }

  // Handle phone change with validation
  const handlePhoneChange = (e) => {
    const value = e.target.value
    // Only allow numeric input
    if (value === "" || /^\d+$/.test(value)) {
      // Prevent starting with 0-5
      if (value.length === 0 || !/^[0-5]/.test(value)) {
        setPhone(value.slice(0, 10)) // Limit to 10 digits
        if (isEditable) {
          setPhoneError(validatePhone(value.slice(0, 10)))
        }
      }
    }
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    setSuccessMessage("")
    setErrorMessage("")

    // Validate inputs
    const nameValidation = validateName(fullName)
    const phoneValidation = validatePhone(phone)

    setNameError(nameValidation)
    setPhoneError(phoneValidation)

    if (nameValidation || phoneValidation || !department) {
      setErrorMessage("Please fix the errors before submitting.")
      return
    }

    try {
      const response = await axios.put(
        `http://localhost:8080/api/faculty/profile`,
        {
          name: fullName,
          department: department,
          designation: designation,
          mobileNo: phone,
        },
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        },
      )

      setSuccessMessage("Profile updated successfully!")
      const updatedFaculty = {
        userId,
        facultyId,
        name: fullName,
        email,
        department,
        designation,
        mobileNo: phone,
      }
      localStorage.setItem("facultyData", JSON.stringify(updatedFaculty))
      setFacultyInfo(updatedFaculty)
      setIsEditable(false)
    } catch (error) {
      setErrorMessage(error.response?.data?.message || "Error updating profile. Please try again later.")
    }
  }

  const requestPasswordChangeOtp = async () => {
    setSuccessMessage("")
    setErrorMessage("")

    try {
      await axios.post(
        `http://localhost:8080/api/faculty/profile/request-password-change`,
        {},
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        },
      )

      setOtpSent(true)
      setSuccessMessage("OTP sent to your email address. Please check your inbox.")
    } catch (error) {
      setErrorMessage("Failed to send OTP. Please try again.")
    }
  }

  const validatePassword = (password) => {
    // Google-style password validation
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

  const handlePasswordChange = async (e) => {
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
      const response = await axios.post(
        `http://localhost:8080/api/faculty/profile/change-password`,
        {
          otp: otp,
          newPassword: newPassword,
        },
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json",
          },
        },
      )
      setSuccessMessage("Password changed successfully!")
      setShowPasswordSection(false)
      setOtpSent(false)
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
      console.error("Password change error:", error.response || error)
      setErrorMessage(
        error.response?.data?.message ||
          error.response?.data ||
          error.message ||
          "Error changing password. Please try again.",
      )
    }
  }

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword)
  }

  if (isLoading) {
    return <p>Loading faculty profile...</p>
  }

  return (
    <div className="fac-profile-page">
      <FacultyNavbar />
      <div className="fac-profile-container">
        <div className="fac-profile-main-content">
          {isModalOpen && (
            <div className="profile-modal">
              <div className="modal-content">
                <h2>Faculty Profile</h2>
                <div className="user-id">
                  <p>Faculty ID: {facultyId}</p>
                </div>
                <div className="user-id">
                  <p>Email: {email}</p>
                </div>

                <form onSubmit={handleUpdate}>
                  <div className="input-group">
                    <label>Full Name</label>
                    <input
                      type="text"
                      value={fullName}
                      onChange={handleNameChange}
                      disabled={!isEditable}
                      className={nameError && isEditable ? "input-error" : ""}
                    />
                    {nameError && isEditable && <p className="validation-error">{nameError}</p>}
                  </div>

                  <div className="input-group">
                    <label>Department</label>
                    <input
                      type="text"
                      value={department}
                      onChange={(e) => setDepartment(e.target.value)}
                      disabled={!isEditable}
                    />
                    {!department && isEditable && <p className="validation-error">Department is required</p>}
                  </div>

                  <div className="input-group">
                  <select value={designation} onChange={(e) => setDesignation(e.target.value)}>
            <option value="">Select Designation</option>
            {designations.map((des) => (
              <option key={des} value={des}>
                {des}
              </option>
            ))}
          </select>
                  </div>

                  <div className="input-group">
                    <label>Phone</label>
                    <input
                      type="text"
                      value={phone}
                      onChange={handlePhoneChange}
                      disabled={!isEditable}
                      className={phoneError && isEditable ? "input-error" : ""}
                      placeholder="10-digit number starting with 6-9"
                    />
                    {phoneError && isEditable && <p className="validation-error">{phoneError}</p>}
                  </div>

                  {isEditable && (
                    <button type="submit" className="update-button">
                      Update
                    </button>
                  )}
                </form>

                {!showPasswordSection ? (
                  <button className="password-button" onClick={() => setShowPasswordSection(true)}>
                    Change Password
                  </button>
                ) : (
                  <div className="password-section">
                    <h3>Change Password</h3>
                    {!otpSent ? (
                      <div>
                        <p>To change your password, we'll send an OTP to your registered email.</p>
                        <button className="request-otp-button" onClick={requestPasswordChangeOtp}>
                          Request OTP
                        </button>
                      </div>
                    ) : (
                      <form onSubmit={handlePasswordChange}>
                        <div className="input-group">
                          <label>OTP</label>
                          <input
                            type="text"
                            value={otp}
                            onChange={(e) => setOtp(e.target.value)}
                            placeholder="Enter OTP from your email"
                          />
                        </div>
                        <div className="input-group password-input-container">
                          <label>New Password</label>
                          <div className="password-field">
                            <input
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
                                  <span className="requirement-text">
                                    At least one special character (e.g., !@#$%^&*)
                                  </span>
                                </li>
                              </ul>
                            </div>
                          )}
                        </div>
                        <button type="submit" className="change-password-button">
                          Change Password
                        </button>
                      </form>
                    )}
                    <button
                      className="cancel-button"
                      onClick={() => {
                        setShowPasswordSection(false)
                        setOtpSent(false)
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
                      }}
                    >
                      Cancel
                    </button>
                  </div>
                )}

                {successMessage && <p className="success-message">{successMessage}</p>}
                {errorMessage && <p className="error-message">{errorMessage}</p>}

                <div className="modal-footer">
                  <button className="close-button" onClick={() => setIsModalOpen(false)}>
                    Close
                  </button>
                  {!isEditable && !showPasswordSection && (
                    <button className="edit-button" onClick={() => setIsEditable(true)}>
                      Edit
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default FacultyProfile

