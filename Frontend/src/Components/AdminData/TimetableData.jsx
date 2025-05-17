"use client"

import { useState, useEffect } from "react"
import FacultyService from "../../Service/FacultyService"
import TimetableService from "../../Service/TimetableService"
import "./TimetableData.css"
import { useNavigate } from "react-router-dom"
import AdminNavbar from "../Land/AdminNavbar"

const TimetableData = () => {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState({ type: "", text: "" })
  const [timetable, setTimetable] = useState([])
  const [batches, setBatches] = useState([])
  const [selectedBatchId, setSelectedBatchId] = useState("")
  const [academicYear, setAcademicYear] = useState("2024-2025")
  const [semester, setSemester] = useState("1")
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
    // Fetch batches for selection
    const fetchData = async () => {
      try {
        const batchesData = await FacultyService.getAllBatches()
        setBatches(batchesData)
      } catch (error) {
        console.error("Error fetching batches:", error)
        setMessage({ type: "error", text: error.message })
      }
    }
    fetchData()
  }, [])

  const handleViewTimetable = async () => {
    setLoading(true)
    setMessage({ type: "", text: "" })

    try {
      if (!selectedBatchId) {
        throw new Error("Please select a batch.")
      }

      const timetableData = await TimetableService.getBatchTimetable(selectedBatchId, academicYear, semester)
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
      <AdminNavbar />
      <div className="view-timetable-container">
        <div className="view-timetable-sidebar">
          
          <div className="card">
            <button className="view-button" onClick={() => handleNavigation("/view-timetable")}>
              View Timetable
            </button>
          </div>
        </div>

        <div className="view-timetable-main-content">
          <h2>View Batch Timetable</h2>
          {message.text && <div className={`message ${message.type}`}>{message.text}</div>}

          <div className="view-timetable-form">
            {/* Batch Selection */}
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

            <button onClick={handleViewTimetable} disabled={loading} className="btn-view">
              {loading ? "Loading..." : "View Timetable"}
            </button>
          </div>

          {/* Timetable Display */}
          {timetable.length > 0 && (
            <div className="timetable-display">
              <h3>Batch Timetable for {batches.find((b) => b.id.toString() === selectedBatchId)?.batchName || "Batch"}</h3>

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
                            <td key={period} className={entry ? (entry.courseType === "ACADEMIC" ? "theory-cell" : "lab-cell") : ""}>
                              {entry ? (
                                <div className="cell-content">
                                  <div className="course-code">{entry.courseCode}</div>
                                  <div className="course-name">{entry.courseName}</div>
                                  <div className="faculty-name">{entry.facultyName}</div>
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

export default TimetableData;
