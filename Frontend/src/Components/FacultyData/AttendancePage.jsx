"use client"
import { useState, useEffect } from "react"
import FacultyNavbar from "../Land/FacultyNavbar"
import FacultyService from "../../Service/FacultyService"
import AttendanceService from "../../Service/AttendanceService"
import "./AttendancePage.css"

const AttendancePage = () => {
  const [courses, setCourses] = useState([])
  const [batches, setBatches] = useState([])
  const [departments, setDepartments] = useState([])
  const [sections, setSections] = useState([])
  const [selectedCourse, setSelectedCourse] = useState("")
  const [selectedBatch, setSelectedBatch] = useState("")
  const [selectedDepartment, setSelectedDepartment] = useState("")
  const [selectedSection, setSelectedSection] = useState("")
  const [attendanceRecords, setAttendanceRecords] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [file, setFile] = useState(null)
  const facultyId = localStorage.getItem("facultyId")

  useEffect(() => {
    // Fetch courses and batches when component mounts
    const fetchData = async () => {
      try {
        setLoading(true)
        const coursesData = await FacultyService.getAllCourses()
        const batchesData = await FacultyService.getAllBatches()
        const assignedCourses = await FacultyService.getAssignedCourses(facultyId)

        // Filter courses to only show assigned ones
        const filteredCourses = coursesData.filter((course) =>
          assignedCourses.some((assigned) => assigned.courseId === course.id),
        )

        // Extract unique departments from batches
        const uniqueDepartments = [...new Set(batchesData.map((batch) => batch.department))].filter(Boolean)

        // Extract unique sections from batches
        const uniqueSections = [...new Set(batchesData.map((batch) => batch.section))].filter(Boolean)

        setCourses(filteredCourses)
        setBatches(batchesData)
        setDepartments(uniqueDepartments)
        setSections(uniqueSections)
      } catch (err) {
        setError("Failed to load data. Please try again.")
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [facultyId])

  // Update sections when department changes
  useEffect(() => {
    if (selectedDepartment) {
      // Filter sections based on selected department
      const filteredSections = [
        ...new Set(batches.filter((batch) => batch.department === selectedDepartment).map((batch) => batch.section)),
      ].filter(Boolean)

      setSections(filteredSections)

      // Reset section if current selection is not valid for new department
      if (selectedSection && !filteredSections.includes(selectedSection)) {
        setSelectedSection("")
      }
    } else {
      // If no department selected, show all sections
      const allSections = [...new Set(batches.map((batch) => batch.section))].filter(Boolean)
      setSections(allSections)
    }
  }, [selectedDepartment, batches])

  const handleCourseChange = (e) => {
    setSelectedCourse(e.target.value)
    setAttendanceRecords([]) // Clear previous records
  }

  const handleBatchChange = (e) => {
    setSelectedBatch(e.target.value)
    setAttendanceRecords([]) // Clear previous records

    // Auto-select department and section if batch has them
    const selectedBatchObj = batches.find((batch) => batch.batchName === e.target.value)
    if (selectedBatchObj) {
      if (selectedBatchObj.department) {
        setSelectedDepartment(selectedBatchObj.department)
      }
      if (selectedBatchObj.section) {
        setSelectedSection(selectedBatchObj.section)
      }
    }
  }

  const handleDepartmentChange = (e) => {
    setSelectedDepartment(e.target.value)
    setAttendanceRecords([]) // Clear previous records
  }

  const handleSectionChange = (e) => {
    setSelectedSection(e.target.value)
    setAttendanceRecords([]) // Clear previous records
  }

  const handleFileChange = (e) => {
    setFile(e.target.files[0])
  }

  const handleGenerateTemplate = async () => {
    if (!selectedCourse || !selectedBatch) {
      setError("Please select both a course and batch")
      return
    }

    try {
      setLoading(true)
      setError("")
      setSuccess("")

      await AttendanceService.generateAttendanceTemplate(
        facultyId,
        selectedCourse,
        selectedBatch,
        selectedDepartment || null,
        selectedSection || null,
      )

      setSuccess("Template generated and downloaded successfully")
    } catch (err) {
      setError(err.message || "Failed to generate template")
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleUploadAttendance = async () => {
    if (!file) {
      setError("Please select a file to upload")
      return
    }

    try {
      setLoading(true)
      setError("")
      setSuccess("")

      const result = await AttendanceService.uploadAttendance(
        facultyId,
        file,
        selectedDepartment || null,
        selectedSection || null,
      )

      setSuccess("Attendance uploaded successfully")
      setFile(null)
      // Reset file input
      document.getElementById("attendance-file").value = ""

      // Refresh attendance records if course and batch are selected
      if (selectedCourse && selectedBatch) {
        fetchAttendanceRecords()
      }
    } catch (err) {
      setError(err.message || "Failed to upload attendance")
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const fetchAttendanceRecords = async () => {
    if (!selectedCourse || !selectedBatch) {
      setError("Please select both a course and batch")
      return
    }

    try {
      setLoading(true)
      setError("")

      const records = await AttendanceService.getAttendanceByFacultyCourseAndBatch(
        facultyId,
        selectedCourse,
        selectedBatch,
        selectedDepartment || null,
        selectedSection || null,
      )

      setAttendanceRecords(records)
    } catch (err) {
      setError(err.message || "Failed to fetch attendance records")
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateReport = async () => {
    if (!selectedCourse || !selectedBatch) {
      setError("Please select both a course and batch")
      return
    }

    try {
      setLoading(true)
      setError("")
      setSuccess("")

      await AttendanceService.generateAttendanceReport(
        facultyId,
        selectedCourse,
        selectedBatch,
        selectedDepartment || null,
        selectedSection || null,
      )

      setSuccess("Report generated and downloaded successfully")
    } catch (err) {
      setError(err.message || "Failed to generate report")
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleViewAttendance = () => {
    fetchAttendanceRecords()
  }

  // Helper function to get course name by ID
  const getCourseName = (courseId) => {
    const course = courses.find((c) => c.id.toString() === courseId.toString())
    if (!course) return courseId

    const courseType =
      course.type === "ACADEMIC"
        ? "Theory"
        : course.type === "NON_ACADEMIC"
          ? "Co-Curricular"
          : course.type === "LAB"
            ? "Lab"
            : course.type

    return `${course.code} - ${course.title} (${courseType})`
  }

  // Get unique batch names to prevent duplicates in dropdown
  const getUniqueBatchNames = () => {
    const uniqueBatchNames = [...new Set(batches.map((batch) => batch.batchName))]
    return uniqueBatchNames
  }

  return (
    <div className="fac-attendance-page">
      <FacultyNavbar />
      <div className="fac-attendance-container">
        <div className="fac-attendance-sidebar">
          <h2>Attendance Management</h2>
          <div className="attendance-actions">
            <button className="action-button template-button" onClick={handleGenerateTemplate} disabled={loading}>
              Generate Template
            </button>

            <button className="action-button view-button" onClick={handleViewAttendance} disabled={loading}>
              View Attendance
            </button>

            <button className="action-button report-button" onClick={handleGenerateReport} disabled={loading}>
              Generate Report
            </button>
          </div>
        </div>

        <div className="fac-attendance-main-content">
          {error && <div className="error-message">{error}</div>}
          {success && <div className="success-message">{success}</div>}

          <div className="attendance-filters">
            <div className="filter-group">
              <label htmlFor="course-select">Select Course:</label>
              <select id="course-select" value={selectedCourse} onChange={handleCourseChange} disabled={loading}>
                <option value="">-- Select Course --</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.code} - {course.title} (
                    {course.type === "ACADEMIC"
                      ? "Theory"
                      : course.type === "NON_ACADEMIC"
                        ? "Co-Curricular"
                        : course.type === "LAB"
                          ? "Lab"
                          : course.type}
                    )
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="batch-select">Select Batch:</label>
              <select id="batch-select" value={selectedBatch} onChange={handleBatchChange} disabled={loading}>
                <option value="">-- Select Batch --</option>
                {getUniqueBatchNames().map((batchName) => (
                  <option key={batchName} value={batchName}>
                    {batchName}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="department-select">Select Department (Optional):</label>
              <select
                id="department-select"
                value={selectedDepartment}
                onChange={handleDepartmentChange}
                disabled={loading}
              >
                <option value="">-- All Departments --</option>
                {departments.map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="section-select">Select Section (Optional):</label>
              <select id="section-select" value={selectedSection} onChange={handleSectionChange} disabled={loading}>
                <option value="">-- All Sections --</option>
                {sections.map((sect) => (
                  <option key={sect} value={sect}>
                    {sect}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="attendance-upload">
            <h2>Upload Attendance</h2>
            <div className="file-upload">
              <input
                type="file"
                id="attendance-file"
                accept=".xlsx, .xls"
                onChange={handleFileChange}
                disabled={loading}
              />
              <button className="upload-button" onClick={handleUploadAttendance} disabled={loading || !file}>
                Upload
              </button>
            </div>
            <p className="upload-note">
              Note: Please download the template first, fill it with attendance data, and then upload.
            </p>
          </div>

          {loading && <div className="loading-spinner">Loading...</div>}

          {attendanceRecords.length > 0 && (
            <div className="attendance-records">
              <h2>Attendance Records</h2>
              <div className="records-info">
                <p>
                  <strong>Course:</strong> {getCourseName(selectedCourse)}
                </p>
                <p>
                  <strong>Batch:</strong> {selectedBatch}
                </p>
                {selectedDepartment && (
                  <p>
                    <strong>Department:</strong> {selectedDepartment}
                  </p>
                )}
                {selectedSection && (
                  <p>
                    <strong>Section:</strong> {selectedSection}
                  </p>
                )}
                <p>
                  <strong>Total Records:</strong> {attendanceRecords.length}
                </p>
              </div>
              <div className="table-container">
                <table className="attendance-table">
                  <thead>
                    <tr>
                      <th>Student ID</th>
                      <th>Student Name</th>
                      <th>Roll No</th>
                      <th>Department</th>
                      <th>Section</th>
                      <th>Total Periods</th>
                      <th>Periods Attended</th>
                      <th>Attendance %</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {attendanceRecords.map((record, index) => (
                      <tr key={index}>
                        <td>{record.studentId}</td>
                        <td>{record.studentName}</td>
                        <td>{record.studentDno}</td>
                        <td>{record.department || "-"}</td>
                        <td>{record.section || "-"}</td>
                        <td>{record.totalPeriods}</td>
                        <td>{record.periodsAttended}</td>
                        <td>{record.attendancePercentage.toFixed(2)}%</td>
                        <td>{record.date}</td>
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

export default AttendancePage
