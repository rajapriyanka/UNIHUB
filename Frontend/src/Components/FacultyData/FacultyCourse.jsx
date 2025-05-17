"use client"

import { useEffect, useState } from "react"
import FacultyService from "../../Service/FacultyService"
import "./FacultyCourse.css"
import FacultyNavbar from "../Land/FacultyNavbar"

const FacultyCourse = () => {
  const [showForm, setShowForm] = useState(false)
  const [showAssignedCourses, setShowAssignedCourses] = useState(false)
  const [courses, setCourses] = useState([])
  const [batches, setBatches] = useState([])
  const [assignedCourses, setAssignedCourses] = useState([])
  const [selectedCourseId, setSelectedCourseId] = useState("")
  const [selectedBatchId, setSelectedBatchId] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [formError, setFormError] = useState(null) // New state for form-specific errors

  // Helper function to get user-friendly course type label
  const getCourseTypeLabel = (type) => {
    switch (type) {
      case "ACADEMIC":
        return "Theory"
      case "NON_ACADEMIC":
        return "Co-Curricular"
      case "LAB":
        return "Lab"
      default:
        return type || "N/A"
    }
  }

  useEffect(() => {
    if (showForm) {
      fetchCoursesAndBatches()
      setFormError(null) // Clear form errors when showing form
    }
    if (showAssignedCourses) {
      fetchAssignedCourses()
    }
  }, [showForm, showAssignedCourses])

  const fetchCoursesAndBatches = async () => {
    try {
      const courses = await FacultyService.getAllCourses()
      const batches = await FacultyService.getAllBatches()
      setCourses(courses)
      setBatches(batches)
    } catch (error) {
      console.error("Error fetching courses and batches:", error)
      setError("Failed to fetch courses and batches. Please try again.")
    }
  }

  const fetchAssignedCourses = async () => {
    setLoading(true)
    try {
      const facultyId = localStorage.getItem("facultyId")
      if (!facultyId) {
        throw new Error("Faculty ID not found. Please log in again.")
      }
      const assignedCoursesData = await FacultyService.getAssignedCourses(facultyId)

      // Enrich the assigned courses with course type information
      const enrichedCourses = assignedCoursesData.map((course) => {
        // Find the full course details from the courses array to get the type
        const fullCourseDetails = courses.find((c) => c.id === course.courseId)
        return {
          ...course,
          type: fullCourseDetails?.type || "ACADEMIC", // Default to ACADEMIC if not found
        }
      })

      setAssignedCourses(enrichedCourses)
    } catch (error) {
      console.error("Error fetching assigned courses:", error)
      setError("Failed to fetch assigned courses. Please try again.")
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async () => {
    setFormError(null) // Clear previous form errors

    if (!selectedCourseId || !selectedBatchId) {
      setFormError("Please select both a course and batch.")
      return
    }

    const facultyId = localStorage.getItem("facultyId")
    if (!facultyId) {
      setFormError("Faculty ID not found. Please log in again.")
      return
    }

    setLoading(true)

    try {
      await FacultyService.addCourseToBatch(facultyId, selectedCourseId, selectedBatchId)
      alert("Course assigned to batch successfully!")
      resetForm()
      // Fetch courses first to ensure we have the course types
      await fetchCoursesAndBatches()
      // Then fetch assigned courses
      fetchAssignedCourses()
    } catch (error) {
      console.error("Error in handleSubmit:", error)

      // Set the error message from the caught error
      setFormError(error.message || "An unexpected error occurred. Please try again later.")
    } finally {
      setLoading(false)
    }
  }

  const resetForm = () => {
    setSelectedCourseId("")
    setSelectedBatchId("")
    setFormError(null)
  }

  const handleRemoveCourse = async (courseId, batchId) => {
    if (
      window.confirm(
        "Are you sure you want to remove this course? This will also remove any related timetable entries.",
      )
    ) {
      try {
        const facultyId = localStorage.getItem("facultyId")
        if (!facultyId) {
          alert("Faculty ID not found. Please log in again.")
          return
        }

        setLoading(true)
        await FacultyService.removeCourse(facultyId, courseId, batchId)
        alert("Course removed successfully!")
        fetchAssignedCourses() // Refresh the list
      } catch (error) {
        console.error("Error removing course:", error)
        alert(error.message || "An unexpected error occurred. Please try again later.")
      } finally {
        setLoading(false)
      }
    }
  }

  return (
    <div className="faculty-course-page">
      <FacultyNavbar />
      <div className="faculty-course-container">
        <div className="faculty-course-sidebar">
          <h2>Faculty Dashboard</h2>
          <button
            onClick={() => {
              setShowForm(!showForm)
              setShowAssignedCourses(false) // Hide assigned courses if form is shown
            }}
            className="sidebar-button"
          >
            {showForm ? "Hide Form" : "Add Course"}
          </button>

          <button
            onClick={() => {
              setShowAssignedCourses(!showAssignedCourses)
              setShowForm(false) // Hide form if assigned courses are shown
              if (!showAssignedCourses) {
                // If we're about to show assigned courses, fetch courses first
                fetchCoursesAndBatches().then(() => fetchAssignedCourses())
              }
            }}
            className="sidebar-button"
          >
            {showAssignedCourses ? "Hide Assigned Courses" : "Assigned Courses"}
          </button>
        </div>

        <div className="faculty-course-main-content">
          {showForm && (
            <div className="card add-course-form">
              <h3>Add New Course</h3>

              {/* Display form error message */}
              {formError && (
                <div
                  className="error-message"
                  style={{
                    color: "red",
                    marginBottom: "15px",
                    padding: "10px",
                    backgroundColor: "#ffeeee",
                    borderRadius: "5px",
                    border: "1px solid #ffcccc",
                  }}
                >
                  {formError}
                </div>
              )}

              <label>Select Course:</label>
              <select
                value={selectedCourseId}
                onChange={(e) => setSelectedCourseId(e.target.value)}
                className="dropdown"
              >
                <option value="">-- Select Course --</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.title} ({course.code}) - {getCourseTypeLabel(course.type)}
                  </option>
                ))}
              </select>

              <label>Select Batch:</label>
              <select value={selectedBatchId} onChange={(e) => setSelectedBatchId(e.target.value)} className="dropdown">
                <option value="">-- Select Batch --</option>
                {batches.map((batch) => (
                  <option key={batch.id} value={batch.id}>
                    {batch.batchName} - {batch.department} ({batch.section})
                  </option>
                ))}
              </select>

              <button onClick={handleSubmit} className="add-course-submit-button" disabled={loading}>
                {loading ? "Processing..." : "Submit"}
              </button>
            </div>
          )}

          {showAssignedCourses && (
            <div className="card-assigned-courses">
              <h3>Assigned Courses</h3>
              {loading && <p>Loading assigned courses...</p>}
              {error && <p className="error-message">{error}</p>}
              {!loading && !error && (
                <table className="assigned-courses-table">
                  <thead>
                    <tr>
                      <th>Course Code</th>
                      <th>Course Title</th>
                      <th>Course Type</th>
                      <th>Batch Name</th>
                      <th>Department</th>
                      <th>Section</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {assignedCourses.map((course, index) => (
                      <tr key={index}>
                        <td>{course.code}</td>
                        <td>{course.title}</td>
                        <td>{getCourseTypeLabel(course.type)}</td>
                        <td>{course.batchName}</td>
                        <td>{course.department}</td>
                        <td>{course.section || "N/A"}</td>
                        <td>
                          <button
                            className="remove-button"
                            onClick={() => handleRemoveCourse(course.courseId, course.batchId)}
                            disabled={loading}
                          >
                            {loading ? "..." : "Remove"}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {!loading && !error && assignedCourses.length === 0 && <p>No courses assigned yet.</p>}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default FacultyCourse

