"use client"

import { useState, useEffect } from "react"
import axios from "axios"
import { format } from "date-fns"
import { useNavigate } from "react-router-dom"
import FacultyNavbar from "../Land/FacultyNavbar"
import "./FacultyLeave.css"
import { toast, ToastContainer } from "react-toastify"
import "react-toastify/dist/ReactToastify.css"

const FacultyLeave = () => {
  const [activeTab, setActiveTab] = useState("request")
  const [facultyList, setFacultyList] = useState([])
  const [leaveHistory, setLeaveHistory] = useState([])
  const [pendingApprovals, setPendingApprovals] = useState([])
  const [currentFacultyId, setCurrentFacultyId] = useState(null)
  const [formData, setFormData] = useState({
    approverId: "", // This is now optional
    subject: "",
    reason: "",
    fromDate: "",
    toDate: "",
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [useAutoApprover, setUseAutoApprover] = useState(true) // New state for auto-approver toggle
  const navigate = useNavigate()
  const [isApprover, setIsApprover] = useState(false)

  // Define the base URL for API calls
  const BASE_URL = "http://localhost:8080" // Update this to match your backend URL

  const DEPARTMENT_APPROVER_EMAILS = {
    "Computer Science and Engineering": "kanmanik135@gmail.com",
    "Electrical Engineering": "ee.hod@university.edu",
    "Mechanical Engineering": "me.hod@university.edu",
    "Civil Engineering": "ce.hod@university.edu",
    "Electronics and Communication": "ece.hod@university.edu",
    "Information Technology": "it.hod@university.edu",
  }

  useEffect(() => {
    // Get current faculty ID from localStorage or context
    const facultyId = localStorage.getItem("facultyId")
    const token = localStorage.getItem("token")

    if (!token || !facultyId) {
      navigate("/login")
      return
    }

    // Set axios default headers
    axios.defaults.headers.common["Authorization"] = `Bearer ${token}`

    setCurrentFacultyId(facultyId)

    // Fetch faculty list for dropdown
    const fetchFacultyList = async () => {
      try {
        setLoading(true)
        console.log("Fetching faculty list...")
        const response = await axios.get(`${BASE_URL}/api/faculty`)
        console.log("Faculty list response:", response.data)
        setFacultyList(response.data.filter((faculty) => faculty.id !== facultyId))

        // Check if current faculty is a department approver
        const currentFaculty = response.data.find((faculty) => faculty.id === Number.parseInt(facultyId))
        if (currentFaculty) {
          const departmentApprovers = Object.values(DEPARTMENT_APPROVER_EMAILS || {})
          const isApprover = currentFaculty.email && departmentApprovers.includes(currentFaculty.email)
          if (isApprover) {
            setIsApprover(true)
          }
        }

        setError("")
      } catch (err) {
        console.error("Error fetching faculty list:", err)
        if (err.response) {
          console.error("Error response:", err.response.status, err.response.data)
          if (err.response.status === 403) {
            toast.error("You don't have permission to view faculty list. Please check your login status.")
            if (err.response.data && err.response.data.message === "Expired or invalid token") {
              localStorage.removeItem("token")
              localStorage.removeItem("facultyId")
              navigate("/login")
            }
          } else {
            toast.error(err.response.data?.message || "Failed to load faculty list")
          }
        } else {
          toast.error("Network error. Please check your connection.")
        }
      } finally {
        setLoading(false)
      }
    }

    // Fetch leave history
    const fetchLeaveHistory = async () => {
      try {
        setLoading(true)
        console.log("Fetching leave history for faculty ID:", facultyId)
        const response = await axios.get(`${BASE_URL}/api/leave/faculty/${facultyId}`)
        console.log("Leave history response:", response.data)
        setLeaveHistory(response.data)
        setError("")
      } catch (err) {
        console.error("Error fetching leave history:", err)
        if (err.response) {
          console.error("Error response:", err.response.status, err.response.data)
          if (err.response.status === 403) {
            toast.error("You don't have permission to view leave history.")
          } else {
            toast.error(err.response.data?.message || "Failed to load leave history")
          }
        } else {
          toast.error("Network error. Please check your connection.")
        }
      } finally {
        setLoading(false)
      }
    }

    // Fetch pending approvals
    const fetchPendingApprovals = async () => {
      try {
        setLoading(true)
        console.log("Fetching pending approvals for faculty ID:", facultyId)
        const response = await axios.get(`${BASE_URL}/api/leave/approver/${facultyId}/pending`)
        console.log("Pending approvals response:", response.data)
        setPendingApprovals(response.data)
        setError("")
      } catch (err) {
        console.error("Error fetching pending approvals:", err)
        if (err.response) {
          console.error("Error response:", err.response.status, err.response.data)
          if (err.response.status === 403) {
            toast.error("You don't have permission to view pending approvals.")
          } else {
            toast.error(err.response.data?.message || "Failed to load pending approvals")
          }
        } else {
          toast.error("Network error. Please check your connection.")
        }
      } finally {
        setLoading(false)
      }
    }

    if (facultyId) {
      fetchFacultyList()
      fetchLeaveHistory()
      fetchPendingApprovals()
    }
  }, [navigate, BASE_URL])

  // Check for email action token in URL
  useEffect(() => {
    const checkForEmailToken = async () => {
      // Parse URL parameters
      const queryParams = new URLSearchParams(window.location.search)
      const token = queryParams.get("token")
      const action = queryParams.get("action")
      const comment = queryParams.get("comment") || ""

      if (token && (action === "approve" || action === "reject")) {
        setLoading(true)
        setError("")

        try {
          // Process the email action
          const result = await axios.get(`${BASE_URL}/api/email-actions/leave/${token}/api`, {
            params: { comment },
          })

          setSuccess(result.data || "Leave request processed successfully!")

          // Remove the token from URL to prevent accidental resubmission
          window.history.replaceState({}, document.title, window.location.pathname)

          // Refresh pending approvals if user is logged in
          if (currentFacultyId) {
            const approvalsResponse = await axios.get(`${BASE_URL}/api/leave/approver/${currentFacultyId}/pending`)
            setPendingApprovals(approvalsResponse.data)
          }
        } catch (err) {
          console.error("Error processing email action:", err)
          setError(err.response?.data || "Failed to process the leave request. The link may be invalid or expired.")
        } finally {
          setLoading(false)
        }
      }
    }

    checkForEmailToken()
  }, [BASE_URL, currentFacultyId])

  const handleInputChange = (e) => {
    const { name, value } = e.target

    // Create a new form data object with the updated field
    const updatedFormData = {
      ...formData,
      [name]: value,
    }

    // Calculate today and 2 months from today for validation
    const today = new Date()
    const twoMonthsFromToday = new Date()
    twoMonthsFromToday.setMonth(today.getMonth() + 2)

    // If changing fromDate, validate it's not more than 2 months from today
    if (name === "fromDate") {
      const selectedFromDate = new Date(value)
      if (selectedFromDate > twoMonthsFromToday) {
        setError("Oops! You can only apply for leave up to 2 months in advance.")
        toast.error("Oops! You can only apply for leave up to 2 months in advance.")
        // Still update the form data to show the invalid selection
      } else {
        // Only clear error if it was related to date range
        if (error === "Oops! You can only apply for leave up to 2 months in advance.") {
          setError("")
        }
      }
    }

    // If changing dates, validate the range (15 days max)
    if (name === "fromDate" || name === "toDate") {
      const fromDate = name === "fromDate" ? value : formData.fromDate
      const toDate = name === "toDate" ? value : formData.toDate

      if (fromDate && toDate && !validateDateRange(fromDate, toDate)) {
        setError("Leave duration cannot exceed 15 days. Ask in person.")
        toast.error("Leave duration cannot exceed 15 days. Ask in person.")

      } else if (error === "Leave duration cannot exceed 15 days. Ask in person.") {
        // Only clear error if it was related to date range and not the 2-month validation
        if (name !== "fromDate" || (name === "fromDate" && new Date(value) <= twoMonthsFromToday)) {
          setError("")
        }
      }
    }

    setFormData(updatedFormData)
  }

  const validateDateRange = (fromDate, toDate) => {
    if (!fromDate || !toDate) return true

    const start = new Date(fromDate)
    const end = new Date(toDate)
    const diffTime = Math.abs(end - start)
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1 // +1 to include both start and end dates

    return diffDays <= 15
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Validate date range before submission
    if (!validateDateRange(formData.fromDate, formData.toDate)) {
      setError("Leave duration cannot exceed 15 days")
      toast.error("Leave duration cannot exceed 15 days")
      return
    }

    // Validate from date is not more than 2 months from today
    const today = new Date()
    const twoMonthsFromToday = new Date()
    twoMonthsFromToday.setMonth(today.getMonth() + 2)
    const selectedFromDate = new Date(formData.fromDate)

    if (selectedFromDate > twoMonthsFromToday) {
      setError("Oops! You can only apply for leave up to 2 months in advance.")
      toast.error("Oops! You can only apply for leave up to 2 months in advance.")
      return
    }

    setLoading(true)
    setError("")
    setSuccess("")

    try {
      console.log("Submitting leave request for faculty ID:", currentFacultyId)

      // If using auto-approver, set approverId to empty string or null
      const requestData = {
        ...formData,
        approverId: useAutoApprover ? "" : formData.approverId,
      }

      console.log("Form data:", requestData)

      const response = await axios.post(`${BASE_URL}/api/leave/request/${currentFacultyId}`, requestData)
      console.log("Leave request response:", response.data)

      setSuccess("Leave request submitted successfully!")
      toast.success("Leave request submitted successfully!")
      setFormData({
        approverId: "",
        subject: "",
        reason: "",
        fromDate: "",
        toDate: "",
      })

      // Check if there was an email issue but request was successful
      if (response.data.error === "Email Service Unavailable") {
        setSuccess("Leave request submitted successfully, but email notification could not be sent.")
      }

      // Refresh leave history
      const historyResponse = await axios.get(`${BASE_URL}/api/leave/faculty/${currentFacultyId}`)
      setLeaveHistory(historyResponse.data)
    } catch (err) {
      console.error("Error submitting leave request:", err)
      if (err.response) {
        console.error("Error response:", err.response.status, err.response.data)
        setError(err.response.data?.message || "Failed to submit leave request. Please try again.")
      } else {
        setError("Network error. Please check your connection.")
      }
    } finally {
      setLoading(false)
    }
  }

  const handleApproveReject = async (leaveId, status, comments = "") => {
    setLoading(true)
    setError("")
    setSuccess("")

    try {
      console.log(`${status} leave request ID:`, leaveId)

      const response = await axios.put(`${BASE_URL}/api/leave/${leaveId}/action/${currentFacultyId}`, {
        status,
        comments,
      })
      console.log("Leave action response:", response.data)

      setSuccess(`Leave request ${status.toLowerCase()} successfully!`)

      // Check if there was an email issue but request was successful
      if (response.data.error === "Email Service Unavailable") {
        setSuccess(`Leave request ${status.toLowerCase()} successfully, but email notification could not be sent.`)
      }

      // Refresh pending approvals
      const approvalsResponse = await axios.get(`${BASE_URL}/api/leave/approver/${currentFacultyId}/pending`)
      setPendingApprovals(approvalsResponse.data)
    } catch (err) {
      console.error(`Error ${status.toLowerCase()}ing leave request:`, err)
      if (err.response) {
        console.error("Error response:", err.response.status, err.response.data)
        setError(err.response.data?.message || `Failed to ${status.toLowerCase()} leave request. Please try again.`)
      } else {
        setError("Network error. Please check your connection.")
      }
    } finally {
      setLoading(false)
    }
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case "APPROVED":
        return "status-badge approved"
      case "REJECTED":
        return "status-badge rejected"
      default:
        return "status-badge pending"
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return ""
    return format(new Date(dateString), "MMM dd, yyyy")
  }

  return (
    <div className="faculty-leave-page">
      <FacultyNavbar />
      <div className="faculty-leave-container">
        <div className="faculty-leave-sidebar">
          <h2>Faculty Leave Management</h2>

          <div className="leave-tabs">
            <button
              className={`tab-button ${activeTab === "request" ? "active" : ""}`}
              onClick={() => setActiveTab("request")}
            >
              Request Leave
            </button>
            <button
              className={`tab-button ${activeTab === "history" ? "active" : ""}`}
              onClick={() => setActiveTab("history")}
            >
              Leave History
            </button>
            <button
              className={`tab-button ${activeTab === "approvals" ? "active" : ""}`}
              onClick={() => setActiveTab("approvals")}
            >
              Pending Approvals
              {pendingApprovals.length > 0 && <span className="badge">{pendingApprovals.length}</span>}
            </button>
          </div>
        </div>

        <div className="faculty-leave-main-content">
          {error && <div className="error-message">{error}</div>}
          {success && <div className="success-message">{success}</div>}

          {activeTab === "request" && (
            <div className="leave-request-form">
              <h2>Submit Leave Request</h2>
              <form onSubmit={handleSubmit}>
                {/* Auto-approver toggle */}
                <div className="form-group auto-approver-toggle">
                  <label className="toggle-container">
                    <input
                      type="checkbox"
                      checked={useAutoApprover}
                      onChange={() => setUseAutoApprover(!useAutoApprover)}
                    />
                    <span className="toggle-label">Automatically send to department approver</span>
                  </label>
                  {useAutoApprover && !isApprover && (
                    <p className="help-text">
                      Your request will be sent to the designated approver for your department.
                    </p>
                  )}
                  {useAutoApprover && isApprover && (
                    <p className="help-text">
                      Since you are a department approver, your request will be sent to an alternative approver
                      automatically.
                    </p>
                  )}
                </div>

                {/* Manual approver selection - only shown when auto-approver is disabled */}
                {!useAutoApprover && (
                  <div className="form-group">
                    <label htmlFor="approverId">Send Request To:</label>
                    <select
                      id="approverId"
                      name="approverId"
                      value={formData.approverId}
                      onChange={handleInputChange}
                      required={!useAutoApprover}
                      disabled={loading || facultyList.length === 0}
                    >
                      <option value="">Select Faculty</option>
                      {facultyList.map((faculty) => (
                        <option key={faculty.id} value={faculty.id}>
                          {faculty.name} ({faculty.email})
                        </option>
                      ))}
                    </select>
                    {facultyList.length === 0 && !loading && !error && (
                      <p className="help-text">No faculty members available to send requests to.</p>
                    )}
                  </div>
                )}

                <div className="form-group">
                  <label htmlFor="subject">Subject:</label>
                  <input
                    type="text"
                    id="subject"
                    name="subject"
                    value={formData.subject}
                    onChange={handleInputChange}
                    required
                    disabled={loading}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="reason">Reason for Leave:</label>
                  <textarea
                    id="reason"
                    name="reason"
                    value={formData.reason}
                    onChange={handleInputChange}
                    required
                    disabled={loading}
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="fromDate">From Date:</label>
                    <input
                      type="date"
                      id="fromDate"
                      name="fromDate"
                      value={formData.fromDate}
                      onChange={handleInputChange}
                      required
                      disabled={loading}
                      min={new Date().toISOString().split("T")[0]}
                      className={
                        formData.fromDate && formData.fromDate < new Date().toISOString().split("T")[0]
                          ? "input-error"
                          : ""
                      }
                    />
                    <small className="form-text">Must be today or a future date (max 2 months from today)</small>
                  </div>

                  <div className="form-group">
                    <label htmlFor="toDate">To Date:</label>
                    <input
                      type="date"
                      id="toDate"
                      name="toDate"
                      value={formData.toDate}
                      onChange={handleInputChange}
                      required
                      disabled={loading}
                      min={formData.fromDate || new Date().toISOString().split("T")[0]}
                      className={
                        (formData.toDate && formData.toDate < formData.fromDate) ||
                        (formData.fromDate && formData.toDate && !validateDateRange(formData.fromDate, formData.toDate))
                          ? "input-error"
                          : ""
                      }
                    />
                    <small className="form-text">Must be on or after From Date and within 15 days of From Date</small>
                  </div>
                </div>

                <button
                  type="submit"
                  className="faculty-leave-submit-button"
                  disabled={loading || (!useAutoApprover && facultyList.length === 0)}
                >
                  {loading ? "Submitting..." : "Submit Leave Request"}
                </button>
              </form>
            </div>
          )}

          {activeTab === "history" && (
            <div className="leave-history">
              <h2>Leave History</h2>
              {loading ? (
                <p className="loading-text">Loading leave history...</p>
              ) : leaveHistory.length === 0 ? (
                <p className="no-data">No leave requests found.</p>
              ) : (
                <div className="leave-table-container">
                  <table className="leave-table">
                    <thead>
                      <tr>
                        <th>Subject</th>
                        <th>Approver</th>
                        <th>From</th>
                        <th>To</th>
                        <th>Requested On</th>
                        <th>Status</th>
                        <th>Comments</th>
                      </tr>
                    </thead>
                    <tbody>
                      {leaveHistory.map((leave) => (
                        <tr key={leave.id}>
                          <td>{leave.subject}</td>
                          <td>{leave.approverName || "Unassigned"}</td>
                          <td>{formatDate(leave.fromDate)}</td>
                          <td>{formatDate(leave.toDate)}</td>
                          <td>{formatDate(leave.requestedAt)}</td>
                          <td>
                            <span className={getStatusBadgeClass(leave.status)}>{leave.status}</span>
                          </td>
                          <td>{leave.comments || "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === "approvals" && (
            <div className="pending-approvals">
              <h2>Pending Leave Approvals</h2>
              {loading ? (
                <p className="loading-text">Loading pending approvals...</p>
              ) : pendingApprovals.length === 0 ? (
                <p className="no-data">No pending approvals found.</p>
              ) : (
                <div className="leave-table-container">
                  <table className="leave-table">
                    <thead>
                      <tr>
                        <th>Faculty</th>
                        <th>Subject</th>
                        <th>From</th>
                        <th>To</th>
                        <th>Requested On</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingApprovals.map((leave) => (
                        <tr key={leave.id}>
                          <td>{leave.facultyName}</td>
                          <td>{leave.subject}</td>
                          <td>{formatDate(leave.fromDate)}</td>
                          <td>{formatDate(leave.toDate)}</td>
                          <td>{formatDate(leave.requestedAt)}</td>
                          <td className="action-buttons">
                            <button
                              className="approve-button"
                              onClick={() => handleApproveReject(leave.id, "APPROVED", "Approved")}
                              disabled={loading}
                            >
                              Approve
                            </button>
                            <button
                              className="reject-button"
                              onClick={() => handleApproveReject(leave.id, "REJECTED", "Rejected")}
                              disabled={loading}
                            >
                              Reject
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
        <ToastContainer position="top-right" autoClose={3000} />
      </div>
    </div>
  )
}

export default FacultyLeave
