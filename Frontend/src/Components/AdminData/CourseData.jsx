"use client"

import { useState, useEffect, useRef } from "react"
import AdminNavbar from "../Land/AdminNavbar"
import UserService from "../../Service/UserService"
import "./CourseData.css"
import * as XLSX from "xlsx"

const CourseData = () => {
  const [showForm, setShowForm] = useState(false)
  const [showList, setShowList] = useState(false)
  const [formData, setFormData] = useState({
    title: "",
    code: "",
    contactPeriods: "",
    semesterNo: "",
    type: "ACADEMIC",
    department: "",
  })
  const [errors, setErrors] = useState({})
  const [courses, setCourses] = useState([])
  const [filteredCourses, setFilteredCourses] = useState([])
  const [searchTerm, setSearchTerm] = useState("")
  const [editingCourse, setEditingCourse] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [selectedSemester, setSelectedSemester] = useState("")
  const [selectedDepartment, setSelectedDepartment] = useState("")
  const [isUploading, setIsUploading] = useState(false)
  const fileInputRef = useRef(null)
  const [message, setMessage] = useState("")

  const courseTypes = ["ACADEMIC", "NON_ACADEMIC", "LAB"]
  const semesters = [1, 2, 3, 4, 5, 6, 7, 8]
  const departments = [
    "Computer Science and Engineering",
    "Electronics and Communication Engineering",
    "Electrical and Electronics Engineering",
    "Mechanical Engineering",
    "Civil Engineering",
    "Information Technology",
    "Aeronautical Engineering",
  ]

  useEffect(() => {
    fetchCourses()
  }, [])

  const fetchCourses = async () => {
    setIsLoading(true)
    try {
      const response = await UserService.getAllCourses()
      if (Array.isArray(response) && response.length > 0) {
        setCourses(response)
        setFilteredCourses(response)
      } else {
        setCourses([])
        setFilteredCourses([])
      }
    } catch (error) {
      console.error("Error fetching courses:", error)
      setCourses([])
      setFilteredCourses([])
    } finally {
      setIsLoading(false)
    }
  }

  const handleRegisterClick = () => {
    setShowForm(true)
    setShowList(false)
    setEditingCourse(null)
    setFormData({
      title: "",
      code: "",
      contactPeriods: "",
      semesterNo: "",
      type: "ACADEMIC",
      department: "",
    })
    setErrors({})
    setMessage("")
  }

  const handleListClick = () => {
    setShowForm(false)
    setShowList(true)
    setSelectedSemester("")
    setSelectedDepartment("")
    setFilteredCourses(courses)
    setMessage("")
  }

  const handleEdit = (course) => {
    setEditingCourse(course)
    setFormData({
      title: course.title,
      code: course.code,
      contactPeriods: course.contactPeriods,
      semesterNo: course.semesterNo,
      type: course.type,
      department: course.department,
    })
    setShowForm(true)
    setShowList(false)
    setErrors({})
    setMessage("")
  }

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this course?")) {
      try {
        await UserService.deleteCourse(id)
        fetchCourses()
        setMessage("Course deleted successfully")
      } catch (error) {
        console.error("Error deleting course:", error)
        setMessage("Error deleting course. Please try again.")
      }
    }
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target

    if (name === "title") {
      if (!/^[A-Za-z\s]*$/.test(value)) {
        return
      }
    }

    if (name === "code") {
      // Block input longer than 7 characters
      if (value.length > 7) return;
  
      // Only allow alphanumeric, must start with letter
      if (
        value === "" ||
        /^[A-Za-z][A-Za-z0-9]*$/.test(value) // First letter, rest alphanumeric
      ) {
        setFormData((prev) => ({ ...prev, [name]: value }));
      }
      return; // Return early to avoid falling through to setFormData again
    }

    if (name === "contactPeriods") {
      if (!/^[1-9]?$/.test(value)) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          [name]: "Contact Periods must be a single digit between 1 and 9",
        }))
        return
      }
      setErrors((prevErrors) => ({ ...prevErrors, [name]: "" }))
    }

    setFormData((prevState) => ({ ...prevState, [name]: value }))
  }

  const validateForm = () => {
    const newErrors = {}
    if (!formData.title || formData.title.trim() === "") {
      newErrors.title = "Title is required"
    } else if (formData.title.trim().length < 5) {
      newErrors.title = "Title should have at least 5 characters"
    } else if (/[^A-Za-z\s]/.test(formData.title)) {
      newErrors.title = "Title should only contain letters and spaces"
    }

    if (!formData.code || formData.code.trim() === "") {
      newErrors.code = "Code is required"
    } else if (formData.code.trim().length < 5) {
      newErrors.code = "Code should have at least 5 characters"
    } else if (formData.code.trim().length > 7) {
      newErrors.code = "Code should have at most 7 characters"
    } else if (!/^[A-Za-z][A-Za-z0-9]*$/.test(formData.code)) {
      newErrors.code = "Code should start with a letter and contain only alphanumeric characters"
    }

    if (!formData.contactPeriods || formData.contactPeriods === "") {
      newErrors.contactPeriods = "Contact Periods is required"
    } else if (!/^[1-9]$/.test(formData.contactPeriods)) {
      newErrors.contactPeriods = "Contact Periods must be a number between 1 and 9"
    }

    if (!formData.semesterNo || formData.semesterNo === "") {
      newErrors.semesterNo = "Semester Number is required"
    } else if (!semesters.includes(Number(formData.semesterNo))) {
      newErrors.semesterNo = "Invalid Semester Number"
    }
    

    if (!formData.department) {
      newErrors.department = "Department is required"
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (validateForm()) {
      try {
        if (editingCourse) {
          await UserService.updateCourse(editingCourse.id, formData)
        } else {
          await UserService.registerCourse(formData)
        }
        setShowForm(false)
        fetchCourses()
        setMessage(editingCourse ? "Course updated successfully" : "Course registered successfully")
      } catch (error) {
        console.error("Error saving course:", error)
        setMessage("Error saving course. Please try again.")
      }
    }
  }

  const handleSearch = async (e) => {
    const value = e.target.value
    setSearchTerm(value)
    if (value.trim() === "") {
      setFilteredCourses(courses)
    } else {
      try {
        const results = await UserService.searchCourses(value)
        setFilteredCourses(results)
      } catch (error) {
        console.error("Error searching courses:", error)
      }
    }
  }

  const handleFilter = () => {
    const filtered = courses.filter((course) => {
      const semesterMatch = !selectedSemester || course.semesterNo.toString() === selectedSemester
      const departmentMatch =
        !selectedDepartment || course.department.toLowerCase() === selectedDepartment.toLowerCase()
      return semesterMatch && departmentMatch
    })
    setFilteredCourses(filtered)
  }

  const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (file) {
      handleExcelUpload(file)
    }
  }

  // Update the handleExcelUpload function to properly map the Excel data to the expected format
  const handleExcelUpload = async (file) => {
    setIsUploading(true)
    setMessage("")

    if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
      setMessage("Please upload a valid Excel file (.xlsx or .xls)")
      setIsUploading(false)
      return
    }

    const reader = new FileReader()

    reader.onload = (event) => {
      const data = new Uint8Array(event.target.result)
      const workbook = XLSX.read(data, { type: "array" })
      const sheetName = workbook.SheetNames[0]
      const sheet = workbook.Sheets[sheetName]
      const jsonData = XLSX.utils.sheet_to_json(sheet)

      console.log("Parsed Excel Data:", jsonData)

      // Validate data before uploading
      const errors = validateExcelData(jsonData)
      if (errors.length > 0) {
        setMessage("Excel file contains errors:\n" + errors.join("\n"))
        setIsUploading(false)
        return
      }

      // Map the data to match the expected format if column names have spaces
      const mappedData = jsonData.map((row) => ({
        title: row.Title,
        code: row.Code,
        contactPeriods: row["Contact Periods"],
        semesterNo: row["Semester No"],
        department: row.Department,
        type: row.Type,
      }))

      console.log("Mapped Data:", mappedData)

      // If valid, proceed with the upload
      uploadExcelData(file, mappedData)
    }

    reader.readAsArrayBuffer(file)
  }

  // Update the uploadExcelData function to handle the mapped data
  const uploadExcelData = async (file, mappedData) => {
    try {
      const formData = new FormData()
      formData.append("file", file)

      // If your API supports sending the parsed data directly, you can add it to the form
      // This is optional and depends on your backend implementation
      if (mappedData) {
        formData.append("parsedData", JSON.stringify(mappedData))
      }

      await UserService.uploadCourseExcel(formData)
      setMessage("Courses uploaded successfully")
      fetchCourses()
    } catch (error) {
      console.error("Error uploading Excel file:", error)
      setMessage(error.response?.data?.message || error.message || "Error uploading courses. Please try again.")
    } finally {
      setIsUploading(false)
    }
  }

  // Update the validateExcelData function to check column order and duplicate course types
  const validateExcelData = (data) => {
    const errors = []

    // Check if the Excel file has the expected columns in the correct order
    if (data.length > 0) {
      const firstRow = data[0]
      const expectedColumns = ["Title", "Code", "Contact Periods", "Semester No", "Department", "Type"]
      const actualColumns = Object.keys(firstRow)

      // Check if all expected columns exist
      const missingColumns = expectedColumns.filter((col) => !actualColumns.includes(col))
      if (missingColumns.length > 0) {
        errors.push(`Missing columns: ${missingColumns.join(", ")}`)
      }

      // Check column order
      let isCorrectOrder = true
      let lastFoundIndex = -1

      for (const expectedCol of expectedColumns) {
        const currentIndex = actualColumns.indexOf(expectedCol)
        if (currentIndex === -1) continue // Skip missing columns as we already reported them

        if (currentIndex <= lastFoundIndex) {
          isCorrectOrder = false
          break
        }
        lastFoundIndex = currentIndex
      }

      if (!isCorrectOrder) {
        errors.push(`Columns are not in the correct order. Expected order: ${expectedColumns.join(", ")}`)
      }
    }

    // Track courses to check for duplicates with same type
    const courseMap = new Map()

    // Continue with the existing validation for each row
    data.forEach((row, index) => {
      const rowNum = index + 2 // Row number in Excel (considering headers)

      // Validate Title (only alphabets and spaces, min 5 chars)
      if (!row.Title || !/^[A-Za-z\s]+$/.test(row.Title) || row.Title.trim().length < 5) {
        errors.push(`Row ${rowNum}: Invalid Title (Only alphabets and spaces, min 5 characters required)`)
      }

      // Validate Code (starts with letter, alphanumeric, 5-7 chars)
      if (
        !row.Code ||
        !/^[A-Za-z][A-Za-z0-9]*$/.test(row.Code) ||
        row.Code.trim().length < 5 ||
        row.Code.trim().length > 7
      ) {
        errors.push(
          `Row ${rowNum}: Invalid Code (Must start with a letter, contain only alphanumeric characters, and be 5-7 characters long)`,
        )
      }

      // Validate Contact Periods (must be a single digit between 1 and 9)
      if (!row["Contact Periods"] || !/^[1-9]$/.test(row["Contact Periods"])) {
        errors.push(`Row ${rowNum}: Invalid Contact Periods (Must be a single digit between 1 and 9)`)
      }

      // Validate Semester Number
      if (!row["Semester No"] || !semesters.includes(row["Semester No"].toString())) {
        errors.push(`Row ${rowNum}: Invalid Semester Number (Must be between 1-8)`)
      }

      // Validate Department
      if (!row.Department || !departments.includes(row.Department)) {
        errors.push(`Row ${rowNum}: Invalid Department`)
      }

      // Validate Course Type
      if (!row.Type || !courseTypes.includes(row.Type)) {
        errors.push(`Row ${rowNum}: Invalid Course Type (Must be ACADEMIC, NON_ACADEMIC, or LAB)`)
      }

      // Check for duplicate courses with the same type
      if (row.Title && row.Code && row.Type) {
        const courseKey = `${row.Title.trim().toLowerCase()}_${row.Code.trim().toLowerCase()}`

        if (courseMap.has(courseKey)) {
          const existingTypes = courseMap.get(courseKey)

          if (existingTypes.includes(row.Type)) {
            errors.push(
              `Row ${rowNum}: Duplicate course "${row.Title}" with code "${row.Code}" and type "${row.Type}" found. Courses can have the same name and code but must have different types.`,
            )
          } else {
            courseMap.get(courseKey).push(row.Type)
          }
        } else {
          courseMap.set(courseKey, [row.Type])
        }
      }
    })

    // Also check for duplicates with existing courses in the database
    if (courses.length > 0 && data.length > 0) {
      data.forEach((row, index) => {
        const rowNum = index + 2

        if (row.Title && row.Code && row.Type) {
          const matchingCourses = courses.filter(
            (course) =>
              course.title.trim().toLowerCase() === row.Title.trim().toLowerCase() &&
              course.code.trim().toLowerCase() === row.Code.trim().toLowerCase() &&
              course.type === row.Type,
          )

          if (matchingCourses.length > 0) {
            errors.push(
              `Row ${rowNum}: Course "${row.Title}" with code "${row.Code}" and type "${row.Type}" already exists in the database.`,
            )
          }
        }
      })
    }

    return errors
  }

  return (
    <div className="course-data-page">
      <AdminNavbar />
      <div className="course-data-container">
        <div className="course-sidebar">
          <h2>Admin Panel</h2>
          <button className="register-button" onClick={handleRegisterClick}>
            Register New Course
          </button>
          <button className="list-button" onClick={handleListClick}>
            List of Courses
          </button>
          <input
            type="file"
            accept=".xlsx, .xls"
            onChange={handleFileChange}
            ref={fileInputRef}
            style={{ display: "none" }}
          />
        </div>

        <div className="course-main-content">
          {message && <div className="message">{message}</div>}
          {showForm && (
            <div className="register-form">
              <h2>{editingCourse ? "Edit Course" : "Register New Course"}</h2>
              <form onSubmit={handleSubmit}>
                <div className="form-group">
                  <label htmlFor="title">Title</label>
                  <input
                    type="text"
                    id="title"
                    name="title"
                    value={formData.title}
                    onChange={handleInputChange}
                    className="form-input"
                  />
                  {errors.title && <span className="error-message">{errors.title}</span>}
                </div>
                <div className="form-group">
                  <label htmlFor="code">Code</label>
                  <input
                    type="text"
                    id="code"
                    name="code"
                    value={formData.code}
                    onChange={handleInputChange}
                    className="form-input"
                  />
                  {errors.code && <span className="error-message">{errors.code}</span>}
                </div>
                <div className="form-group">
                  <label htmlFor="contactPeriods">Contact Periods Per Week</label>
                  <input
                    type="text"
                    id="contactPeriods"
                    name="contactPeriods"
                    value={formData.contactPeriods}
                    onChange={handleInputChange}
                    className="form-input"
                  />
                  {errors.contactPeriods && <span className="error-message">{errors.contactPeriods}</span>}
                </div>
                <div className="form-group">
                  <label htmlFor="semesterNo">Semester Number</label>
                  <select
                    id="semesterNo"
                    name="semesterNo"
                    value={formData.semesterNo}
                    onChange={handleInputChange}
                    className="form-input"
                  >
                    <option value="">Select Semester</option>
                    {semesters.map((semester) => (
                      <option key={semester} value={semester}>
                        {semester}
                      </option>
                    ))}
                  </select>
                  {errors.semesterNo && <span className="error-message">{errors.semesterNo}</span>}
                </div>
                <div className="form-group">
                  <label htmlFor="type">Course Type</label>
                  <select
                    id="type"
                    name="type"
                    value={formData.type}
                    onChange={handleInputChange}
                    className="form-input"
                  >
                    <option value="ACADEMIC">Theory</option>
                    <option value="NON_ACADEMIC">Co-curricular</option>
                    <option value="LAB">Lab</option>
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="department">Department</label>
                  <select
                    id="department"
                    name="department"
                    value={formData.department}
                    onChange={handleInputChange}
                    className="form-input"
                  >
                    <option value="">Select Department</option>
                    {departments.map((dept) => (
                      <option key={dept} value={dept}>
                        {dept}
                      </option>
                    ))}
                  </select>
                  {errors.department && <span className="error-message">{errors.department}</span>}
                </div>

                <div className="form-actions">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current.click()}
                    disabled={isUploading}
                    className="upload-button"
                  >
                    {isUploading ? "Uploading..." : "Upload Excel"}
                  </button>
                  <button type="submit" className="register-submit-button">
                    {editingCourse ? "Update" : "Register"}
                  </button>
                  <button
                    type="button"
                    onClick={
                      editingCourse
                        ? handleListClick
                        : () => {
                            setFormData({
                              title: "",
                              code: "",
                              contactPeriods: "",
                              semesterNo: "",
                              type: "ACADEMIC",
                              department: "",
                            })
                            setErrors({})
                          }
                    }
                    className="cancel-button"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          )}

          {showList && (
            <div className="course-list">
              <div className="filter-controls">
                <select
                  value={selectedSemester}
                  onChange={(e) => setSelectedSemester(e.target.value)}
                  className="semester-select"
                >
                  <option value="">All Semesters</option>
                  {semesters.map((semester) => (
                    <option key={semester} value={semester}>
                      Semester {semester}
                    </option>
                  ))}
                </select>
                <select
                  value={selectedDepartment}
                  onChange={(e) => setSelectedDepartment(e.target.value)}
                  className="department-select"
                >
                  <option value="">All Departments</option>
                  {departments.map((department) => (
                    <option key={department} value={department}>
                      {department}
                    </option>
                  ))}
                </select>
                <button onClick={handleFilter} className="filter-button">
                  Filter
                </button>
              </div>
              <div className="course-search-bar">
                <input
                  type="text"
                  placeholder="Search course by title or code"
                  value={searchTerm}
                  onChange={handleSearch}
                  className="course-search-input"
                />
              </div>
              {isLoading ? (
                <p>Loading course data...</p>
              ) : filteredCourses.length === 0 ? (
                <p>No course data available.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Code</th>
                      <th>Contact Periods</th>
                      <th>Semester No</th>
                      <th>Type</th>
                      <th>Department</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredCourses.map((course) => (
                      <tr key={course.id}>
                        <td>{course.title}</td>
                        <td>{course.code}</td>
                        <td>{course.contactPeriods}</td>
                        <td>{course.semesterNo}</td>
                        <td>
                          {course.type === "ACADEMIC"
                            ? "Theory"
                            : course.type === "NON_ACADEMIC"
                              ? "Co-Curricular"
                              : "Lab"}
                        </td>
                        <td>{course.department || "N/A"}</td>
                        <td>
                          <div className="action-buttons">
                            <button className="edit-button" onClick={() => handleEdit(course)}>
                              Edit
                            </button>
                            <button className="delete-button" onClick={() => handleDelete(course.id)}>
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default CourseData
