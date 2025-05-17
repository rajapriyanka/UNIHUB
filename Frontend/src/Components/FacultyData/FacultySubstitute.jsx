"use client"

import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { useFaculty } from "../../Context/FacultyContext"
import SubstituteService from "../../Service/SubstituteService"
import TimetableService from "../../Service/TimetableService"
import FacultyService from "../../Service/FacultyService"

import { toast, ToastContainer } from "react-toastify"
import "react-toastify/dist/ReactToastify.css"
import { format, addMonths, isAfter, getDay } from "date-fns"
import "./FacultySubstitute.css"
import FacultyNavbar from "../Land/FacultyNavbar"

const FacultySubstitute = () => {
  const { facultyData } = useFaculty()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [timetableEntries, setTimetableEntries] = useState([])
  const [batches, setBatches] = useState([])
  const [selectedEntry, setSelectedEntry] = useState(null)
  const [selectedDate, setSelectedDate] = useState("")
  const [reason, setReason] = useState("")
  const [availableFaculty, setAvailableFaculty] = useState([])
  const [selectedSubstitute, setSelectedSubstitute] = useState("")
  const [filterByAvailability, setFilterByAvailability] = useState(true)
  const [filterByBatch, setFilterByBatch] = useState(false)
  const [activeTab, setActiveTab] = useState("request")
  const [sentRequests, setSentRequests] = useState([])
  const [receivedRequests, setReceivedRequests] = useState([])
  const [debug, setDebug] = useState({
    facultyData: null,
    timetableLoaded: false,
    batchesLoaded: false,
    error: null,
  })

  // Map day names to day numbers (0 = Sunday, 1 = Monday, etc.)
  const dayToNumber = {
    SUNDAY: 0,
    MONDAY: 1,
    TUESDAY: 2,
    WEDNESDAY: 3,
    THURSDAY: 4,
    FRIDAY: 5,
    SATURDAY: 6,
  }

  // Debug effect to log state changes
  useEffect(() => {
    console.log("Faculty Data:", facultyData)
    console.log("Timetable Entries:", timetableEntries)
    console.log("Batches:", batches)
    setDebug((prev) => ({
      ...prev,
      facultyData: facultyData,
    }))
  }, [facultyData, timetableEntries, batches])

  useEffect(() => {
    console.log("Component mounted")

    if (!facultyData) {
      console.log("No faculty data, redirecting to login")
      navigate("/faculty/login")
      return
    }

    console.log("Faculty data available:", facultyData)

    // Only fetch data if facultyData exists and has an id
    if (facultyData && facultyData.facultyId) {
      console.log("Fetching initial data with faculty ID:", facultyData.facultyId)
      fetchInitialData()
    } else {
      console.error("Faculty data exists but ID is missing:", facultyData)
      setDebug((prev) => ({
        ...prev,
        error: "Faculty ID is missing",
      }))
    }
  }, [facultyData, navigate])

  useEffect(() => {
    // Only proceed if facultyData and facultyData.id exist
    if (!facultyData || !facultyData.facultyId) {
      console.log("Skipping tab-related data fetch due to missing faculty data")
      return
    }

    console.log("Tab changed to:", activeTab)

    if (activeTab === "sent") {
      console.log("Fetching sent requests for faculty ID:", facultyData.facultyId)
      fetchSentRequests()
    } else if (activeTab === "received") {
      console.log("Fetching received requests for faculty ID:", facultyData.facultyId)
      fetchReceivedRequests()
    }
  }, [activeTab, facultyData])

  const fetchInitialData = async () => {
    console.log("Starting fetchInitialData")
    setLoading(true)

    try {
      // Check if facultyData and facultyData.id exist before making API calls
      if (!facultyData || !facultyData.facultyId) {
        console.error("Faculty data is not available for fetchInitialData")
        toast.error("Faculty data is not available. Please log in again.")
        navigate("/faculty/login")
        return
      }

      console.log("Fetching timetable for faculty ID:", facultyData.facultyId)
      console.log("Fetching all batches")

      // Fetch timetable and batches separately to better identify which call might be failing
      try {
        const timetableData = await TimetableService.getFacultyTimetable(facultyData.facultyId)
        console.log("Timetable data received:", timetableData)
        setTimetableEntries(timetableData)
        setDebug((prev) => ({
          ...prev,
          timetableLoaded: true,
        }))
      } catch (timetableError) {
        console.error("Error fetching timetable:", timetableError)
        toast.error("Failed to load timetable: " + timetableError.message)
      }

      try {
        const batchesData = await FacultyService.getAllBatches()
        console.log("Batches data received:", batchesData)
        setBatches(batchesData)
        setDebug((prev) => ({
          ...prev,
          batchesLoaded: true,
        }))
      } catch (batchesError) {
        console.error("Error fetching batches:", batchesError)
        toast.error("Failed to load batches: " + batchesError.message)
      }
    } catch (error) {
      console.error("General error in fetchInitialData:", error)
      toast.error("Failed to load initial data: " + error.message)
      setDebug((prev) => ({
        ...prev,
        error: error.message,
      }))
    } finally {
      setLoading(false)
      console.log("Completed fetchInitialData")
    }
  }

  const fetchSentRequests = async () => {
    console.log("Starting fetchSentRequests")
    setLoading(true)
    try {
      // Check if facultyData and facultyData.id exist before making API calls
      if (!facultyData || !facultyData.facultyId) {
        console.error("Faculty data is not available for fetchSentRequests")
        toast.error("Faculty data is not available. Please log in again.")
        navigate("/faculty/login")
        return
      }

      console.log("Fetching sent requests for faculty ID:", facultyData.facultyId)
      const requests = await SubstituteService.getRequestsByRequester(facultyData.facultyId)
      console.log("Sent requests received:", requests)
      setSentRequests(requests)
    } catch (error) {
      console.error("Error in fetchSentRequests:", error)
      toast.error("Failed to load sent requests: " + error.message)
    } finally {
      setLoading(false)
      console.log("Completed fetchSentRequests")
    }
  }

  const fetchReceivedRequests = async () => {
    console.log("Starting fetchReceivedRequests")
    setLoading(true)
    try {
      // Check if facultyData and facultyData.id exist before making API calls
      if (!facultyData || !facultyData.facultyId) {
        console.error("Faculty data is not available for fetchReceivedRequests")
        toast.error("Faculty data is not available. Please log in again.")
        navigate("/faculty/login")
        return
      }

      console.log("Fetching received requests for faculty ID:", facultyData.facultyId)
      const requests = await SubstituteService.getRequestsBySubstitute(facultyData.facultyId)
      console.log("Received requests received:", requests)
      setReceivedRequests(requests)
    } catch (error) {
      console.error("Error in fetchReceivedRequests:", error)
      toast.error("Failed to load received requests: " + error.message)
    } finally {
      setLoading(false)
      console.log("Completed fetchReceivedRequests")
    }
  }

  const handleEntrySelect = (entry) => {
    console.log("Selected entry:", entry)
    setSelectedEntry(entry)
    setSelectedSubstitute("")
    setAvailableFaculty([])

    // Reset selected date when entry changes
    setSelectedDate("")
  }

  // Function to get valid dates for the selected entry
  const getValidDatesForEntry = (entry) => {
    if (!entry || !entry.day) return []

    const today = new Date()
    today.setHours(0, 0, 0, 0) // Reset time to start of day

    const twoMonthsFromToday = addMonths(today, 2)
    const dayNumber = dayToNumber[entry.day.toUpperCase()]

    if (dayNumber === undefined) {
      console.error("Invalid day in entry:", entry.day)
      return []
    }

    const validDates = []
    const currentDate = new Date(today)

    // Find the next occurrence of the day
    while (getDay(currentDate) !== dayNumber) {
      currentDate.setDate(currentDate.getDate() + 1)
    }

    // Collect all valid dates (same day of week) within the 2-month period
    while (!isAfter(currentDate, twoMonthsFromToday)) {
      validDates.push(new Date(currentDate))
      currentDate.setDate(currentDate.getDate() + 7) // Move to next week
    }

    return validDates
  }

  const handleDateChange = (e) => {
    const selectedDate = e.target.value
    const today = new Date()
    today.setHours(0, 0, 0, 0) // Reset to start of day

    const selectedDateObj = new Date(selectedDate)
    selectedDateObj.setHours(0, 0, 0, 0) // Reset to start of day

    console.log("Selected date:", selectedDate)

    // Validate that the selected date is not in the past
    if (selectedDateObj < today) {
      toast.error("Cannot select a date in the past. Please choose today or a future date.")
      setSelectedDate("")
      return
    }

    // Validate that the selected date is not more than 2 months from today
    const twoMonthsFromToday = addMonths(today, 2)
    if (selectedDateObj > twoMonthsFromToday) {
      toast.error("Cannot select a date more than 2 months from today.")
      setSelectedDate("")
      return
    }

    // Validate that the selected date matches the day of the week of the selected class
    if (selectedEntry && selectedEntry.day) {
      const dayNumber = dayToNumber[selectedEntry.day.toUpperCase()]
      if (dayNumber !== undefined && getDay(selectedDateObj) !== dayNumber) {
        toast.error(`Selected date must be a ${selectedEntry.day} to match the class schedule.`)
        setSelectedDate("")
        return
      }
    }

    setSelectedDate(selectedDate)
    setSelectedSubstitute("")
    setAvailableFaculty([])
  }

  // Function to extract batch ID from the selected entry
  const extractBatchId = (entry) => {
    if (!entry) return null

    // Log the entry to see its structure
    console.log("Extracting batch ID from entry:", entry)

    // Try different possible locations for the batch ID
    if (entry.batchId !== undefined && entry.batchId !== null) {
      console.log("Found batchId directly:", entry.batchId)
      return entry.batchId
    }

    if (entry.batch && entry.batch.id !== undefined && entry.batch.id !== null) {
      console.log("Found batch.id:", entry.batch.id)
      return entry.batch.id
    }

    // If we have a batch name, try to find the corresponding batch ID from the batches array
    if (entry.batchName && batches.length > 0) {
      const matchingBatch = batches.find(
        (batch) => batch.batchName === entry.batchName || batch.name === entry.batchName,
      )

      if (matchingBatch) {
        console.log("Found matching batch by name:", matchingBatch.id)
        return matchingBatch.id
      }
    }

    // If we have a batch code, try to find the corresponding batch ID
    if (entry.batchCode && batches.length > 0) {
      const matchingBatch = batches.find((batch) => batch.code === entry.batchCode)

      if (matchingBatch) {
        console.log("Found matching batch by code:", matchingBatch.id)
        return matchingBatch.id
      }
    }

    console.log("Could not extract batch ID from entry")
    return null
  }

  const handleFilterFaculty = async () => {
    if (!selectedEntry || !selectedDate) {
      toast.warning("Please select a timetable entry and date")
      return
    }

    console.log("Starting handleFilterFaculty")
    console.log("Selected entry:", selectedEntry)
    console.log("Selected date:", selectedDate)

    // Extract the batch ID from the selected entry
    const batchId = extractBatchId(selectedEntry)
    console.log("Extracted batch ID:", batchId)

    // Check if trying to filter by batch but no batchId is available
    if (filterByBatch && !batchId) {
      toast.warning("Cannot filter by batch handling because the selected entry has no batch ID")
      setFilterByBatch(false) // Automatically disable the filter
      return
    }

    setLoading(true)
    try {
      // Check if facultyData and facultyData.id exist before making API calls
      if (!facultyData || !facultyData.facultyId) {
        console.error("Faculty data is not available for handleFilterFaculty")
        toast.error("Faculty data is not available. Please log in again.")
        navigate("/faculty/login")
        return
      }

      const filterData = {
        requestingFacultyId: facultyData.facultyId,
        requestDate: selectedDate,
        periodNumber: selectedEntry.periodNumber,
        day: selectedEntry.day, // Add the day parameter
        batchId: batchId, // Use the extracted batch ID
        filterByAvailability,
        filterByBatch,
      }

      console.log("Filter data:", filterData)
      const faculty = await SubstituteService.filterFaculty(filterData)
      console.log("Available faculty received:", faculty)
      setAvailableFaculty(faculty)

      if (faculty.length === 0) {
        toast.info("No faculty members match your filter criteria")
      }
    } catch (error) {
      console.error("Error in handleFilterFaculty:", error)
      toast.error("Failed to filter faculty: " + error.message)
    } finally {
      setLoading(false)
      console.log("Completed handleFilterFaculty")
    }
  }

  const handleSubmitRequest = async () => {
    if (!selectedEntry || !selectedDate || !selectedSubstitute || !reason) {
      toast.warning("Please fill all required fields")
      return
    }

    // Additional validation for date
    const today = new Date()
    today.setHours(0, 0, 0, 0) // Reset to start of day

    const selectedDateObj = new Date(selectedDate)
    selectedDateObj.setHours(0, 0, 0, 0) // Reset to start of day

    if (selectedDateObj < today) {
      toast.error("Cannot request substitution for a past date. Please select today or a future date.")
      return
    }

    // Validate that the selected date is not more than 2 months from today
    const twoMonthsFromToday = addMonths(today, 2)
    if (selectedDateObj > twoMonthsFromToday) {
      toast.error("Cannot request substitution for a date more than 2 months from today.")
      return
    }

    // Validate that the selected date matches the day of the week of the selected class
    if (selectedEntry && selectedEntry.day) {
      const dayNumber = dayToNumber[selectedEntry.day.toUpperCase()]
      if (dayNumber !== undefined && getDay(selectedDateObj) !== dayNumber) {
        toast.error(`Selected date must be a ${selectedEntry.day} to match the class schedule.`)
        return
      }
    }

    console.log("Starting handleSubmitRequest")
    console.log("Selected entry:", selectedEntry)
    console.log("Selected date:", selectedDate)
    console.log("Selected substitute:", selectedSubstitute)
    console.log("Reason:", reason)
    console.log("Faculty data being used:", facultyData)

    setLoading(true)
    try {
      // Check if facultyData and facultyData.id exist before making API calls
      if (!facultyData || !facultyData.facultyId) {
        console.error("Faculty data is not available for handleSubmitRequest")
        toast.error("Faculty data is not available. Please log in again.")
        navigate("/faculty/login")
        return
      }

      console.log("Selected entry details:", {
        id: selectedEntry.id,
        day: selectedEntry.day,
        periodNumber: selectedEntry.periodNumber,
        batchId: extractBatchId(selectedEntry),
      })

      // Create a more detailed request object with all necessary information
      const requestData = {
        requesterId: Number(facultyData.facultyId),
        substituteId: Number(selectedSubstitute),
        timetableEntryId: Number(selectedEntry.id),
        requestDate: selectedDate,
        day: selectedEntry.day, // Add the day parameter
        reason,
      }

      console.log("Request data being sent:", requestData)

      try {
        const response = await SubstituteService.createSubstituteRequest(requestData)
        console.log("Response from create request:", response)
        toast.success("Substitute request sent successfully")

        // Reset form
        setSelectedEntry(null)
        setSelectedDate("")
        setReason("")
        setSelectedSubstitute("")
        setAvailableFaculty([])

        // Switch to sent requests tab
        setActiveTab("sent")
        fetchSentRequests()
      } catch (error) {
        // Handle the specific error about secret key
        if (error.message && error.message.includes("secret key byte array cannot be null or empty")) {
          toast.error(
            "Email service is currently unavailable. Your request has been recorded but email notifications could not be sent.",
          )

          // Still reset the form and switch tabs as the request might have been created
          setSelectedEntry(null)
          setSelectedDate("")
          setReason("")
          setSelectedSubstitute("")
          setAvailableFaculty([])
          setActiveTab("sent")
          fetchSentRequests()
        } else {
          throw error // Re-throw other errors to be caught by the outer catch block
        }
      }
    } catch (error) {
      console.error("Error in handleSubmitRequest:", error)
      console.error("Error details:", error.message, error.stack)
      toast.error("Failed to send request: " + error.message)
    } finally {
      setLoading(false)
      console.log("Completed handleSubmitRequest")
    }
  }

  const handleUpdateRequestStatus = async (requestId, status, responseMessage = "") => {
    console.log("Starting handleUpdateRequestStatus")
    console.log("Request ID:", requestId)
    console.log("Status:", status)
    console.log("Response message:", responseMessage)

    setLoading(true)
    try {
      try {
        await SubstituteService.updateRequestStatus(requestId, status, responseMessage)
        toast.success(`Request ${status.toLowerCase()} successfully`)
      } catch (error) {
        // Handle the specific error about secret key
        if (error.message && error.message.includes("secret key byte array cannot be null or empty")) {
          toast.warning(`Request ${status.toLowerCase()}, but email notification could not be sent.`)
        } else {
          throw error // Re-throw other errors to be caught by the outer catch block
        }
      }
      fetchReceivedRequests()
    } catch (error) {
      console.error("Error in handleUpdateRequestStatus:", error)
      toast.error(`Failed to ${status.toLowerCase()} request: ` + error.message)
    } finally {
      setLoading(false)
      console.log("Completed handleUpdateRequestStatus")
    }
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case "PENDING":
        return "status-badge pending"
      case "APPROVED":
        return "status-badge approved"
      case "REJECTED":
        return "status-badge rejected"
      default:
        return "status-badge"
    }
  }

  const formatDate = (dateString) => {
    try {
      return format(new Date(dateString), "EEEE, MMMM d, yyyy")
    } catch (error) {
      return dateString
    }
  }

  // Add this useEffect to handle batch filter state based on selected entry
  useEffect(() => {
    if (selectedEntry) {
      // Extract the batch ID from the selected entry
      const batchId = extractBatchId(selectedEntry)

      // If selected entry has no batch ID, disable the batch filter
      if (!batchId && filterByBatch) {
        console.log("Automatically disabling batch filter because selected entry has no batch ID")
        setFilterByBatch(false)
      }
    }
  }, [selectedEntry, filterByBatch, batches])

  // Check if the selected entry has a valid batch ID
  const hasBatchId = selectedEntry ? extractBatchId(selectedEntry) !== null : false

  // Get valid dates for the selected entry
  const validDates = selectedEntry ? getValidDatesForEntry(selectedEntry) : []

  // Format valid dates for display
  const validDatesFormatted = validDates.map((date) => date.toISOString().split("T")[0])

  // Calculate max date (2 months from today)
  const today = new Date()
  const maxDate = addMonths(today, 2).toISOString().split("T")[0]

  return (
    <div className="fac-subs-page">
      <FacultyNavbar />
      <div className="fac-subs-container">
        <div className="fac-subs-sidebar">
          <button className={activeTab === "request" ? "active" : ""} onClick={() => setActiveTab("request")}>
            Request Substitute
          </button>
          <button className={activeTab === "sent" ? "active" : ""} onClick={() => setActiveTab("sent")}>
            Sent Requests
          </button>
          <button className={activeTab === "received" ? "active" : ""} onClick={() => setActiveTab("received")}>
            Received Requests
          </button>
        </div>
        <div className="fac-subs-main-content">
          <h2>Faculty Substitution Management</h2>
          {activeTab === "request" && (
            <div className="request-form">
              <div className="form-section">
                <h3>Step 1: Select Class Details</h3>
                <div className="form-group">
                  <label>Select Timetable Entry:</label>
                  <select
                    value={selectedEntry ? selectedEntry.id : ""}
                    onChange={(e) => {
                      const entry = timetableEntries.find((entry) => entry.id.toString() === e.target.value)
                      handleEntrySelect(entry)
                    }}
                  >
                    <option value="">-- Select Class --</option>
                    {timetableEntries.map((entry) => (
                      <option key={entry.id} value={entry.id}>
                        {entry.courseName} ({entry.courseCode}) - {entry.batchName} - {entry.day} Period{" "}
                        {entry.periodNumber}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label>Select Date for Substitution:</label>
                  <input
                    type="date"
                    value={selectedDate}
                    onChange={handleDateChange}
                    min={new Date().toISOString().split("T")[0]}
                    max={maxDate}
                    required
                    disabled={!selectedEntry}
                  />
                  {selectedEntry && (
                    <small className="form-text">Please select a {selectedEntry.day} within the next 2 months</small>
                  )}
                  {selectedEntry && validDates.length > 0 && (
                    <div className="valid-dates-info" style={{ marginTop: "10px", fontSize: "14px", color: "#666" }}>
                      <p>Valid dates for this class:</p>
                      <ul style={{ marginTop: "5px", paddingLeft: "20px" }}>
                        {validDates.slice(0, 5).map((date, index) => (
                          <li key={index}>{format(date, "EEE, MMM d, yyyy")}</li>
                        ))}
                        {validDates.length > 5 && <li>...and {validDates.length - 5} more</li>}
                      </ul>
                    </div>
                  )}
                </div>

                <div className="form-group">
                  <label>Reason for Substitution:</label>
                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Please provide a reason for your substitution request"
                    rows={3}
                  />
                </div>
              </div>

              <div className="form-section">
                <h3>Step 2: Find Available Faculty</h3>
                <div className="filter-options">
                  <div className="checkbox-group">
                    <input
                      type="checkbox"
                      id="filterAvailability"
                      checked={filterByAvailability}
                      onChange={() => setFilterByAvailability(!filterByAvailability)}
                    />
                    <label htmlFor="filterAvailability">Filter by Availability</label>
                  </div>
                  <div className="checkbox-group">
                    <input
                      type="checkbox"
                      id="filterBatch"
                      checked={filterByBatch}
                      onChange={() => setFilterByBatch(!filterByBatch)}
                      disabled={!hasBatchId}
                    />
                    <label htmlFor="filterBatch">
                      Filter by Batch Handling
                      {selectedEntry && !hasBatchId && (
                        <span className="form-text" style={{ color: "#f44336", marginLeft: "5px" }}>
                          (No batch ID available)
                        </span>
                      )}
                    </label>
                  </div>
                </div>
                <button
                  className="filter-button"
                  onClick={handleFilterFaculty}
                  disabled={!selectedEntry || !selectedDate}
                >
                  Find Available Faculty
                </button>

                {availableFaculty.length > 0 && (
                  <div className="faculty-list">
                    <h4>Available Faculty Members</h4>
                    <div className="faculty-table-container" style={{ overflowX: "auto", marginTop: "20px" }}>
                      <table
                        className="faculty-table"
                        style={{ width: "100%", borderCollapse: "collapse", backgroundColor: "white" }}
                      >
                        <thead>
                          <tr>
                            <th
                              style={{ backgroundColor: "#013c28", color: "white", padding: "12px", textAlign: "left" }}
                            >
                              Name
                            </th>
                            <th
                              style={{ backgroundColor: "#013c28", color: "white", padding: "12px", textAlign: "left" }}
                            >
                              Department
                            </th>
                            <th
                              style={{ backgroundColor: "#013c28", color: "white", padding: "12px", textAlign: "left" }}
                            >
                              Designation
                            </th>
                            <th
                              style={{ backgroundColor: "#013c28", color: "white", padding: "12px", textAlign: "left" }}
                            >
                              Available
                            </th>
                            <th
                              style={{ backgroundColor: "#013c28", color: "white", padding: "12px", textAlign: "left" }}
                            >
                              Handles Batch
                            </th>
                            <th
                              style={{ backgroundColor: "#013c28", color: "white", padding: "12px", textAlign: "left" }}
                            >
                              Action
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {availableFaculty.map((faculty) => (
                            <tr
                              key={faculty.facultyId}
                              style={{ backgroundColor: "#f9f9f9", borderBottom: "1px solid #ddd" }}
                            >
                              <td style={{ padding: "12px", textAlign: "left" }}>{faculty.name}</td>
                              <td style={{ padding: "12px", textAlign: "left" }}>{faculty.department}</td>
                              <td style={{ padding: "12px", textAlign: "left" }}>{faculty.designation}</td>
                              <td style={{ padding: "12px", textAlign: "left" }}>
                                <span
                                  style={{
                                    display: "inline-block",
                                    padding: "4px 8px",
                                    borderRadius: "4px",
                                    backgroundColor: faculty.available ? "#e8f5e9" : "#ffebee",
                                    color: faculty.available ? "#2e7d32" : "#c62828",
                                    fontWeight: "500",
                                    fontSize: "14px",
                                  }}
                                >
                                  {faculty.available ? "Yes" : "No"}
                                </span>
                              </td>
                              <td style={{ padding: "12px", textAlign: "left" }}>
                                <span
                                  style={{
                                    display: "inline-block",
                                    padding: "4px 8px",
                                    borderRadius: "4px",
                                    backgroundColor: faculty.handlesBatch ? "#e8f5e9" : "#ffebee",
                                    color: faculty.handlesBatch ? "#2e7d32" : '#c62828",  : "#ffebee',
                                    color: faculty.handlesBatch ? "#2e7d32" : "#c62828",
                                    fontWeight: "500",
                                    fontSize: "14px",
                                  }}
                                >
                                  {faculty.handlesBatch ? "Yes" : "No"}
                                </span>
                              </td>
                              <td style={{ padding: "12px", textAlign: "left" }}>
                                {selectedSubstitute === faculty.facultyId ? (
                                  <button
                                    className="select-button"
                                    style={{
                                      backgroundColor: "#025d1f",
                                      color: "#fff",
                                      border: "none",
                                      borderRadius: "4px",
                                      padding: "8px 16px",
                                      cursor: "default",
                                      fontWeight: "500",
                                    }}
                                    disabled
                                  >
                                    Selected
                                  </button>
                                ) : (
                                  <button
                                    className="select-button"
                                    style={{
                                      backgroundColor: "#013c28",
                                      color: "#fff",
                                      border: "none",
                                      borderRadius: "4px",
                                      padding: "8px 16px",
                                      cursor: faculty.available ? "pointer" : "not-allowed",
                                      opacity: faculty.available ? "1" : "0.6",
                                      fontWeight: "500",
                                    }}
                                    onClick={() => {
                                      console.log("Selected faculty:", faculty)
                                      setSelectedSubstitute(faculty.facultyId)
                                      toast.success(`Selected ${faculty.name} as substitute`)
                                      // Scroll to the Step 3 section
                                      document.querySelector(".form-section:nth-child(3)").scrollIntoView({
                                        behavior: "smooth",
                                        block: "start",
                                      })
                                    }}
                                    disabled={!faculty.available}
                                  >
                                    Select
                                  </button>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>

              <div className="form-section">
                <h3>Step 3: Submit Request</h3>
                <button
                  className="submit-button"
                  onClick={handleSubmitRequest}
                  disabled={!selectedEntry || !selectedDate || !selectedSubstitute || !reason || loading}
                >
                  {loading ? "Submitting..." : "Submit Substitute Request"}
                </button>
              </div>
            </div>
          )}

          {activeTab === "sent" && (
            <div className="requests-list">
              <h3>Sent Substitute Requests</h3>
              {loading ? (
                <p>Loading requests...</p>
              ) : sentRequests.length === 0 ? (
                <p>No substitute requests sent.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Course</th>
                      <th>Batch</th>
                      <th>Date</th>
                      <th>Period</th>
                      <th>Substitute</th>
                      <th>Status</th>
                      <th>Response</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sentRequests.map((request) => (
                      <tr key={request.id}>
                        <td>
                          {request.courseTitle} ({request.courseCode})
                        </td>
                        <td>
                          {request.batchName} {request.section}
                        </td>
                        <td>{formatDate(request.requestDate)}</td>
                        <td>
                          Period {request.periodNumber} ({request.startTime} - {request.endTime})
                        </td>
                        <td>{request.substituteName}</td>
                        <td>
                          <span className={getStatusBadgeClass(request.status)}>{request.status}</span>
                        </td>
                        <td>{request.responseMessage || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {activeTab === "received" && (
            <div className="requests-list">
              <h3>Received Substitute Requests</h3>
              {loading ? (
                <p>Loading requests...</p>
              ) : receivedRequests.length === 0 ? (
                <p>No substitute requests received.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Requester</th>
                      <th>Course</th>
                      <th>Batch</th>
                      <th>Date</th>
                      <th>Period</th>
                      <th>Reason</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {receivedRequests.map((request) => (
                      <tr key={request.id}>
                        <td>{request.requesterName}</td>
                        <td>
                          {request.courseTitle} ({request.courseCode})
                        </td>
                        <td>
                          {request.batchName} {request.section}
                        </td>
                        <td>{formatDate(request.requestDate)}</td>
                        <td>
                          Period {request.periodNumber} ({request.startTime} - {request.endTime})
                        </td>
                        <td>{request.reason}</td>
                        <td>
                          <span className={getStatusBadgeClass(request.status)}>{request.status}</span>
                        </td>
                        <td>
                          {request.status === "PENDING" && (
                            <div className="action-buttons">
                              <button
                                className="approve-button"
                                onClick={() =>
                                  handleUpdateRequestStatus(request.id, "APPROVED", "I can substitute for this class.")
                                }
                                disabled={loading}
                              >
                                Approve
                              </button>
                              <button
                                className="reject-button"
                                onClick={() =>
                                  handleUpdateRequestStatus(
                                    request.id,
                                    "REJECTED",
                                    "I'm not available for this substitution.",
                                  )
                                }
                                disabled={loading}
                              >
                                Reject
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
        <ToastContainer position="top-right" autoClose={3000} />
      </div>
    </div>
  )
}

export default FacultySubstitute
