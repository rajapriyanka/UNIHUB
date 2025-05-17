"use client"

import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import "react-toastify/dist/ReactToastify.css"
import { toast, ToastContainer } from "react-toastify"
import { format, differenceInDays } from "date-fns"
import axios from "axios"
import "./StudentLeaveData.css"
import StudentNavbar from "../Land/StudentNavbar"


const BASE_URL = "http://localhost:8080"

const StudentLeaveData = () => {
  const navigate = useNavigate()
  const studentId = localStorage.getItem("studentId")

  const [activeTab, setActiveTab] = useState("request")
  const [faculties, setFaculties] = useState([])
  const [leaveHistory, setLeaveHistory] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [dateError, setDateError] = useState("")
  const [formData, setFormData] = useState({
    facultyId: "",
    subject: "",
    reason: "",
    fromDate: null,
    toDate: null,
  })

  useEffect(() => {
    if (!studentId) {
      toast.error("Please login to access this page")
      navigate("/login")
      return
    }

    const fetchFaculties = async () => {
      try {
        const token = localStorage.getItem("token")
        const response = await axios.get(`${BASE_URL}/api/faculty`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        setFaculties(response.data)
      } catch (error) {
        toast.error(error.response?.data?.message || "Failed to fetch faculties")
      }
    }

    const fetchLeaveHistory = async () => {
      try {
        setIsLoading(true)
        const token = localStorage.getItem("token")
        if (!studentId) {
          toast.error("Student ID not found")
          setIsLoading(false)
          return
        }
        const response = await axios.get(`${BASE_URL}/api/student-leave/student/${studentId}`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        setLeaveHistory(response.data)
      } catch (error) {
        toast.error(error.response?.data?.message || "Failed to fetch leave history")
      } finally {
        setIsLoading(false)
      }
    }

    fetchFaculties()
    if (studentId) {
      fetchLeaveHistory()
    }
  }, [studentId, navigate])

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData({
      ...formData,
      [name]: value,
    })
  }

  const validateDateRange = (fromDate, toDate) => {
    if (!fromDate || !toDate) return true

    const start = new Date(fromDate)
    const end = new Date(toDate)
    const diffTime = Math.abs(end - start)
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1 // +1 to include both start and end dates

    return diffDays <= 15
  }

  // Calculate the date 2 months from today
  const getTwoMonthsFromToday = () => {
    const today = new Date()
    const twoMonthsFromToday = new Date()
    twoMonthsFromToday.setMonth(today.getMonth() + 2)
    twoMonthsFromToday.setHours(0, 0, 0, 0) // Reset to start of day
    return twoMonthsFromToday
  }

  const handleDateChange = (e) => {
    const { name, value } = e.target

    // Create updated form data
    const updatedFormData = {
      ...formData,
      [name]: value,
    }

    if (name === "fromDate") {
      const selectedDate = new Date(value)
      const today = new Date()
      today.setHours(0, 0, 0, 0) // Reset to start of day
      const twoMonthsFromToday = getTwoMonthsFromToday()

      if (selectedDate < today) {
        toast.error("From date cannot be in the past")
        return
      }

      if (selectedDate > twoMonthsFromToday) {
        toast.error("From date cannot be more than 2 months from today")
        setDateError("From date cannot be more than 2 months from today")
        return
      } else {
        // Only clear the date error if it was related to the 2-month restriction
        if (dateError === "From date cannot be more than 2 months from today") {
          setDateError("")
        }
      }
    }

    // Validate range between fromDate and toDate
    if ((name === "fromDate" || name === "toDate") && updatedFormData.fromDate && updatedFormData.toDate) {
      if (!validateDateRange(updatedFormData.fromDate, updatedFormData.toDate)) {
        setDateError("Leave duration cannot exceed 15 days. Please contact faculty in person for longer leaves.")
        toast.error("Leave duration cannot exceed 15 days. Please contact faculty in person for longer leaves.")
      } else if (
        dateError === "Leave duration cannot exceed 15 days. Please contact faculty in person for longer leaves."
      ) {
        setDateError("")
      }
    }

    setFormData(updatedFormData)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Validate form data
    if (!formData.facultyId) {
      toast.error("Please select a faculty")
      return
    }

    if (!formData.subject || !formData.reason) {
      toast.error("Please fill all required fields")
      return
    }

    if (!formData.fromDate || !formData.toDate) {
      toast.error("Please select both from and to dates")
      return
    }

    // Validate that from date is not more than 2 months from today
    const selectedFromDate = new Date(formData.fromDate)
    const twoMonthsFromToday = getTwoMonthsFromToday()

    if (selectedFromDate > twoMonthsFromToday) {
      setDateError("From date cannot be more than 2 months from today")
      toast.error("From date cannot be more than 2 months from today")
      return
    }

    // Validate that leave request is not more than 15 days
    if (!validateDateRange(formData.fromDate, formData.toDate)) {
      setDateError("Leave duration cannot exceed 15 days. Please contact faculty in person for longer leaves.")
      toast.error("Leave duration cannot exceed 15 days")
      return
    }

    try {
      setIsLoading(true)
      const token = localStorage.getItem("token")

      await axios.post(`${BASE_URL}/api/student-leave/request/${studentId}`, formData, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      })

      toast.success("Leave request submitted successfully")

      // Reset form
      setFormData({
        facultyId: "",
        subject: "",
        reason: "",
        fromDate: null,
        toDate: null,
      })
      setDateError("")

      // Refresh leave history
      const response = await axios.get(`${BASE_URL}/api/student-leave/student/${studentId}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      setLeaveHistory(response.data)

      // Switch to history tab
      setActiveTab("history")
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to submit leave request")
    } finally {
      setIsLoading(false)
    }
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case "PENDING":
        return (
          <span className="px-2 py-1 rounded-full text-xs bg-yellow-100 text-yellow-800 border border-yellow-300">
            <span className="inline-block w-3 h-3 mr-1">⏱️</span> Pending
          </span>
        )
      case "APPROVED":
        return (
          <span className="px-2 py-1 rounded-full text-xs bg-green-100 text-green-800 border border-green-300">
            <span className="inline-block w-3 h-3 mr-1">✅</span> Approved
          </span>
        )
      case "REJECTED":
        return (
          <span className="px-2 py-1 rounded-full text-xs bg-red-100 text-red-800 border border-red-300">
            <span className="inline-block w-3 h-3 mr-1">❌</span> Rejected
          </span>
        )
      default:
        return <span className="px-2 py-1 rounded-full text-xs border">{status}</span>
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return "N/A"
    const date = new Date(dateString)
    return format(date, "PPP") // Format: Jan 1, 2021
  }

  // Calculate max date for from date input (2 months from today)
  const maxFromDate = getTwoMonthsFromToday().toISOString().split("T")[0]

  return (
    <div className="stud-leave-page">
      <StudentNavbar />
      <div className="stud-leave-container">
        <div className="stud-leave-sidebar">
          <h2>Student Leave Management</h2>
          <div className="stud-leave-sidebar-btns">
            <button
              className={` ${activeTab === "request" ? "stud-leave-req-btn" : ""}`}
              onClick={() => setActiveTab("request")}
            >
              Request Leave
            </button>
            <button
              className={` ${activeTab === "history" ? "stud-leave-his-btn" : ""}`}
              onClick={() => setActiveTab("history")}
            >
              Leave History
            </button>
          </div>
        </div>

        <div className="stud-leave-main-content">
          {activeTab === "request" ? (
            <div className="stud-leave-form">
              <h2>Student Leave Request Form</h2>
              <form onSubmit={handleSubmit}>
                <div className="space-y-4 mt-4">
                  <div className="form-group">
                    <label htmlFor="facultyId" className="block text-sm font-medium">
                      Faculty
                    </label>
                    <select
                      id="facultyId"
                      name="facultyId"
                      value={formData.facultyId}
                      onChange={handleInputChange}
                      className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                      required
                    >
                      <option value="">Select Faculty</option>
                      {faculties.map((faculty) => (
                        <option key={faculty.id} value={faculty.id}>
                          {faculty.name} - {faculty.department}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label htmlFor="subject" className="block text-sm font-medium">
                      Subject
                    </label>
                    <input
                      id="subject"
                      name="subject"
                      type="text"
                      value={formData.subject}
                      onChange={handleInputChange}
                      placeholder="Enter subject"
                      className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="reason" className="block text-sm font-medium">
                      Reason
                    </label>
                    <textarea
                      id="reason"
                      name="reason"
                      value={formData.reason}
                      onChange={handleInputChange}
                      placeholder="Enter reason for leave"
                      rows={4}
                      className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                      required
                    />
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label htmlFor="fromDate" className="block text-sm font-medium">
                        From Date
                      </label>
                      <input
                        id="fromDate"
                        name="fromDate"
                        type="date"
                        value={formData.fromDate || ""}
                        onChange={handleDateChange}
                        min={new Date().toISOString().split("T")[0]} // Set minimum date to today
                        max={maxFromDate} // Set maximum date to 2 months from today
                        className={`w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                          dateError && dateError.includes("From date") ? "border-red-500 bg-red-50" : ""
                        }`}
                        required
                      />
                      <small className="text-xs text-gray-500">
                        Must be between today and {format(new Date(maxFromDate), "MMM dd, yyyy")}
                      </small>
                    </div>

                    <div className="form-group">
                      <label htmlFor="toDate" className="block text-sm font-medium">
                        To Date
                      </label>
                      <input
                        id="toDate"
                        name="toDate"
                        type="date"
                        value={formData.toDate || ""}
                        onChange={handleDateChange}
                        min={formData.fromDate || ""}
                        className={`w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                          dateError && dateError.includes("Leave duration") ? "border-red-500 bg-red-50" : ""
                        }`}
                        required
                      />
                    </div>
                  </div>

                  {dateError && (
                    <div className="error-message bg-red-100 text-red-800 p-3 rounded border border-red-300 mb-4">
                      <strong>Error:</strong> {dateError}
                    </div>
                  )}

                  <div className="form-group">
                    <p className="text-sm text-gray-600 italic">
                      Note: Leave requests cannot exceed 15 days and must start within the next 2 months.
                    </p>
                  </div>
                </div>

                <div className="stud-leave-submit-btn">
                  <button
                    type="submit"
                    disabled={isLoading || dateError !== ""}
                    className={`w-full py-2 px-4 text-white rounded focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                      isLoading || dateError !== ""
                        ? "bg-gray-400 cursor-not-allowed"
                        : "bg-blue-500 hover:bg-blue-600 focus:ring-blue-500"
                    }`}
                  >
                    {isLoading ? "Submitting..." : "Submit Leave Request"}
                  </button>
                </div>
              </form>
            </div>
          ) : (
            <div className="stud-leave-history-container">
              <h2 className="text-xl font-bold mb-4 text-center">Student Leave Request Form</h2>
              {isLoading ? (
                <div className="text-center py-8">Loading leave history...</div>
              ) : leaveHistory.length === 0 ? (
                <div className="text-center py-8">No leave requests found.</div>
              ) : (
                <div className="stud-leave-table-container">
                  <table className="stud-leave-table">
                    <thead>
                      <tr className="bg-gray-100">
                        <th className="p-2 text-left">Subject</th>
                        <th className="p-2 text-left">Faculty</th>
                        <th className="p-2 text-left">From</th>
                        <th className="p-2 text-left">To</th>
                        <th className="p-2 text-left">Days</th>
                        <th className="p-2 text-left">Status</th>
                        <th className="p-2 text-left">Comments</th>
                      </tr>
                    </thead>
                    <tbody>
                      {leaveHistory.map((leave) => {
                        const fromDate = new Date(leave.fromDate)
                        const toDate = new Date(leave.toDate)
                        const daysDifference = differenceInDays(toDate, fromDate) + 1

                        return (
                          <tr key={leave.id} className="border-b hover:bg-gray-50">
                            <td className="p-2">{leave.subject}</td>
                            <td className="p-2">{leave.facultyName}</td>
                            <td className="p-2">{formatDate(leave.fromDate)}</td>
                            <td className="p-2">{formatDate(leave.toDate)}</td>
                            <td className="p-2">
                              {daysDifference} day{daysDifference !== 1 ? "s" : ""}
                            </td>
                            <td className="p-2">{getStatusBadge(leave.status)}</td>
                            <td className="p-2">{leave.comments || "No comments"}</td>
                          </tr>
                        )
                      })}
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

export default StudentLeaveData
