"use client"

import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import FacultyService from "../../Service/FacultyService"
import TimetableService from "../../Service/TimetableService"
import "./Timetable.css"
import FacultyNavbar from "../Land/FacultyNavbar";

const TimetableGeneration = () => {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState({ type: "", text: "" })
  const [faculties, setFaculties] = useState([])
  const [selectedFacultyId, setSelectedFacultyId] = useState("")
  const [academicYear, setAcademicYear] = useState("2024-2025")
  const [semester, setSemester] = useState("FALL")
  const navigate = useNavigate()

  useEffect(() => {
    // Check if user is faculty
    if (!FacultyService.isFaculty()) {
      setMessage({ type: "error", text: "Only faculty members can access this page." })
      return
    }

    // If current user is faculty, set their ID as selected
    const currentFacultyId = localStorage.getItem("facultyId")
    if (currentFacultyId) {
      setSelectedFacultyId(currentFacultyId)
    }

    // Fetch faculties for admin users
    const fetchFaculties = async () => {
      try {
        const response = await FacultyService.getAllFaculties()
        setFaculties(response)
      } catch (error) {
        console.error("Error fetching faculties:", error)
        setMessage({ type: "error", text: error.message })
      }
    }

    // Only fetch all faculties if user is admin
    const userRole = localStorage.getItem("role")
    if (userRole?.toUpperCase() === "ADMIN") {
      fetchFaculties()
    }
  }, [])

  const handleGenerateTimetable = async () => {
    setLoading(true)
    setMessage({ type: "", text: "" })

    try {
      // Use the selected faculty ID or the current user's faculty ID
      const facultyId = selectedFacultyId || localStorage.getItem("facultyId")

      if (!facultyId) {
        throw new Error("No faculty selected.")
      }

      await TimetableService.generateTimetable(facultyId, academicYear, semester)
      setMessage({
        type: "success",
        text: "Timetable generated successfully! You can now view it in the View Timetable section.",
      })
    } catch (error) {
      console.error("Error generating timetable:", error)
      setMessage({ type: "error", text: error.message })
    } finally {
      setLoading(false)
    }
  }

  const handleNavigation = (path) => {
    navigate(path)
  }

  return (
    <div className="timetable-page">
    <FacultyNavbar />
    <div className="timetable-container">
      <div className="timetable-sidebar">
       <h2>Faculty Schedule</h2>
          <div className="card">
            
            <button className="generate-button" onClick={() => handleNavigation("/generate-timetable")}>
              Generate Timetable
            </button>
          </div>
          <div className="card">
            
            <button className="view-button" onClick={() => handleNavigation("/view-timetable")}>
              View Timetable
            </button>
          </div>
      </div>
      <div className="timetable-main-content">
      <h2>Generate Timetable</h2>

      {message.text && <div className={`message ${message.type}`}>{message.text}</div>}

      <div className="timetable-form">
        {/* Faculty Selection (only for admin) */}
        {localStorage.getItem("role")?.toUpperCase() === "ADMIN" && (
          <div className="form-group">
            <label>Select Faculty:</label>
            <select
              value={selectedFacultyId}
              onChange={(e) => setSelectedFacultyId(e.target.value)}
              className="form-control"
            >
              <option value="">-- Select Faculty --</option>
              {faculties.map((faculty) => (
                <option key={faculty.id} value={faculty.id}>
                  {faculty.name} - {faculty.department}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Academic Year Selection */}
        <div className="form-group">
          <label>Academic Year:</label>
          <select value={academicYear} onChange={(e) => setAcademicYear(e.target.value)} className="form-control">
            <option value="2022-2023">2022-2023</option>
            <option value="2023-2024">2023-2024</option>
            <option value="2024-2025">2024-2025</option>
          </select>
        </div>

        {/* Semester Selection */}
        <div className="form-group">
          <label>Semester:</label>
          <select value={semester} onChange={(e) => setSemester(e.target.value)} className="form-control">
            <option value="1">1</option>
            <option value="2">2</option>
            <option value="3">3</option>
            <option value="4">4</option>
            <option value="5">5</option>
            <option value="6">6</option>
            <option value="7">7</option>
            <option value="8">8</option>
          </select>
        </div>

        <button onClick={handleGenerateTimetable} disabled={loading} className="btn-generate">
          {loading ? "Generating..." : "Generate Timetable"}
        </button>
      </div>

      <div className="timetable-info">
        <h3>Timetable Generation Rules:</h3>
        <ul>
          <li>Each faculty will have at least one 1st, 2nd, and last period in a week</li>
          <li>Lab periods will not be scheduled in the 1st period</li>
          <li>Lab periods will be scheduled continuously</li>
          <li>The system will check for conflicts with other faculty schedules</li>
          <li>The timetable will follow the standard 8-period day structure</li>
        </ul>
      </div>
      </div>
    </div>
    </div>
  )
}

export default TimetableGeneration;

