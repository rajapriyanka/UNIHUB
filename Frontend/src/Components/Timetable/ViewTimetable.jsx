"use client"

import { useState, useEffect } from "react"
import FacultyService from "../../Service/FacultyService"
import TimetableService from "../../Service/TimetableService"
import "./ViewTimetable.css"
import { useNavigate } from "react-router-dom"
import FacultyNavbar from "../Land/FacultyNavbar"

const ViewTimetable = () => {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState({ type: "", text: "" })
  const [timetable, setTimetable] = useState([])
  const [viewType, setViewType] = useState("faculty") // 'faculty' or 'batch'
  const [faculties, setFaculties] = useState([])
  const [batches, setBatches] = useState([])
  const [selectedFacultyId, setSelectedFacultyId] = useState("")
  const [selectedBatchId, setSelectedBatchId] = useState("")
  const [academicYear, setAcademicYear] = useState("2024-2025")
  const [semester, setSemester] = useState("FALL")
  const navigate = useNavigate()

  // Days and periods for the timetable
  const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"]
  const periods = [1, 2, 3, 4, 5, 6, 7, 8]
  const periodTimes = [
    "9:00 - 9:50",
    "9:50 - 10:40",
    "10:50 - 11:40",
    "11:40 - 12:30",
    "1:10 - 2:00",
    "2:00 - 2:50",
    "3:00 - 3:50",
    "3:50 - 4:40",
  ]

  useEffect(() => {
    // Set current faculty ID as selected if user is faculty
    const currentFacultyId = localStorage.getItem("facultyId")
    if (currentFacultyId) {
      setSelectedFacultyId(currentFacultyId)
    }

    // Fetch faculties and batches
    const fetchData = async () => {
      try {
        const batchesData = await FacultyService.getAllBatches()
        setBatches(batchesData)

        // Only fetch all faculties if user is admin
        if (localStorage.getItem("role")?.toUpperCase() === "ADMIN") {
          const facultiesData = await FacultyService.getAllFaculties()
          setFaculties(facultiesData)
        }
      } catch (error) {
        console.error("Error fetching data:", error)
        setMessage({ type: "error", text: error.message })
      }
    }

    fetchData()
  }, [])

  // Load timetable when component mounts if faculty user
  useEffect(() => {
    const currentFacultyId = localStorage.getItem("facultyId")
    if (currentFacultyId && FacultyService.isFaculty()) {
      handleViewTimetable()
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleViewTimetable = async () => {
    setLoading(true)
    setMessage({ type: "", text: "" })

    try {
      let timetableData

      if (viewType === "faculty") {
        // Use the selected faculty ID or the current user's faculty ID
        const facultyId = selectedFacultyId || localStorage.getItem("facultyId")

        if (!facultyId) {
          throw new Error("No faculty selected.")
        }

        timetableData = await TimetableService.getFacultyTimetable(facultyId)
      } else {
        if (!selectedBatchId) {
          throw new Error("No batch selected.")
        }

        timetableData = await TimetableService.getBatchTimetable(selectedBatchId, academicYear, semester)
      }

      setTimetable(timetableData)

      if (timetableData.length === 0) {
        setMessage({
          type: "info",
          text: "No timetable entries found. Please generate a timetable first.",
        })
      }
    } catch (error) {
      console.error("Error fetching timetable:", error)
      setMessage({ type: "error", text: error.message })
    } finally {
      setLoading(false)
    }
  }

  // Organize timetable data by day and period
  const organizedTimetable = days.reduce((acc, day) => {
    acc[day] = {}
    periods.forEach((period) => {
      acc[day][period] = timetable.find((entry) => entry.day === day && entry.periodNumber === period)
    })
    return acc
  }, {})

  const handleNavigation = (path) => {
    navigate(path)
  }

  return (
    <div className="view-timetable-page">
      <FacultyNavbar />
    <div className="view-timetable-container">
      
      <div className="view-timetable-sidebar">
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

    <div className="view-timetable-main-content">
      <h2>View Timetable</h2>  
      {message.text && <div className={`message ${message.type}`}>{message.text}</div>}

      <div className="view-timetable-form">
        {/* View Type Selection */}
        <div className="form-group">
          <label>View Type:</label>
          <div className="radio-group">
            <label>
              <input
                type="radio"
                value="faculty"
                checked={viewType === "faculty"}
                onChange={() => setViewType("faculty")}
              />
              Faculty Timetable
            </label>
            <label>
              <input type="radio" value="batch" checked={viewType === "batch"} onChange={() => setViewType("batch")} />
              Batch Timetable
            </label>
          </div>
        </div>

        {/* Faculty Selection (only for admin in faculty view) */}
        {viewType === "faculty" && localStorage.getItem("role")?.toUpperCase() === "ADMIN" && (
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

        {/* Batch Selection (for batch view) */}
        {viewType === "batch" && (
          <div className="form-group">
            <label>Select Batch:</label>
            <select
              value={selectedBatchId}
              onChange={(e) => setSelectedBatchId(e.target.value)}
              className="form-control"
            >
              <option value="">-- Select Batch --</option>
              {batches.map((batch) => (
                <option key={batch.id} value={batch.id}>
                  {batch.batchName} - {batch.department} {batch.section}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Academic Year and Semester (for batch view) */}
        {viewType === "batch" && (
          <>
            <div className="form-group">
              <label>Academic Year:</label>
              <select value={academicYear} onChange={(e) => setAcademicYear(e.target.value)} className="form-control">
                <option value="2022-2023">2024-2025</option>
                <option value="2023-2024">2023-2024</option>
                <option value="2024-2025">2024-2025</option>
              </select>
            </div>

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
          </>
        )}

        <button onClick={handleViewTimetable} disabled={loading} className="btn-view">
          {loading ? "Loading..." : "View Timetable"}
        </button>
      </div>

      {timetable.length > 0 && (
        <div className="timetable-display">
          <h3>
            {viewType === "faculty"
              ? `Faculty Timetable${selectedFacultyId ? ` for ${faculties.find((f) => f.id.toString() === selectedFacultyId)?.name || "Faculty"}` : ""}`
              : `Batch Timetable for ${batches.find((b) => b.id.toString() === selectedBatchId)?.batchName || "Batch"}`}
          </h3>

          <div className="timetable-scroll">
            <table className="timetable">
              <thead>
                <tr>
                  <th>Day/Period</th>
                  {periods.map((period, index) => (
                    <th key={period}>
                      {period} <br />
                      <span className="period-time">{periodTimes[index]}</span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {days.map((day) => (
                  <tr key={day}>
                    <td className="day-cell">{day}</td>
                    {periods.map((period) => {
                      const entry = organizedTimetable[day][period]
                      return (
                        <td
                          key={period}
                          className={entry?.courseType === "ACADEMIC" ? "theory-cell" : entry ? "lab-cell" : "" }
                        >
                          {entry ? (
                            <div className="cell-content">
                              <div className="course-code">{entry.courseCode}</div>
                              <div className="course-name">{entry.courseName}</div>
                              {viewType !== "faculty" && <div className="faculty-name">{entry.facultyName}</div>}
                              {viewType !== "batch" && (
                                <div className="batch-name">
                                  {entry.batchName} {entry.section}
                                </div>
                              )}
                            </div>
                          ) : null}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
      </div>
    </div>
    </div>
  )
}

export default ViewTimetable;

