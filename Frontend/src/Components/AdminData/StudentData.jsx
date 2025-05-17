"use client"

import { useState, useEffect, useRef } from "react"
import UserService from "../../Service/UserService"
import AdminNavbar from "../Land/AdminNavbar"
import "./StudentData.css"
import * as XLSX from "xlsx"

const StudentData = () => {
  const [students, setStudents] = useState([])
  const [formData, setFormData] = useState({
    name: "",
    user: { email: "", password: "" },
    dno: "",
    department: "",
    batchName: "",
    mobileNumber: "",
    section: "", // Added section field with empty default
  })
  const [isEditing, setIsEditing] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [searchTerm, setSearchTerm] = useState("")
  const [isUploading, setIsUploading] = useState(false)
  const [activeTab, setActiveTab] = useState(null)
  const [error, setError] = useState("")
  const fileInputRef = useRef(null)
  const [selectedDepartment, setSelectedDepartment] = useState("")
  const [batchInput, setBatchInput] = useState("")
  const [filteredStudents, setFilteredStudents] = useState([])
  const [errors, setErrors] = useState({})
  const [allStudents, setAllStudents] = useState([]) // Store all students for local filtering

  useEffect(() => {
    fetchStudents()
  }, [])

  const fetchStudents = async () => {
    try {
      const data = await UserService.getAllStudents()
      setStudents(data)
      setAllStudents(data) // Store all students for local filtering
      setFilteredStudents(data)
    } catch (error) {
      console.error("Error fetching students:", error)
      setError("Failed to fetch students. Please try again.")
    }
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target;

    // Clear the error for this field when user starts typing
    if (errors[name]) {
        setErrors((prev) => ({ ...prev, [name]: "" }));
    }

    if (name === "email") {
        setFormData((prev) => ({
            ...prev,
            user: { ...prev.user, [name]: value },
        }));
    } else if (name === "password") {
        // When password changes, update both password and mobile number
        setFormData((prev) => ({
            ...prev,
            user: { ...prev.user, password: value },
            mobileNumber: value, // Sync mobile number with password
        }));
    } else if (name === "mobileNumber") {
        // Prevent mobile number from starting with 0 to 5 and limit to 10 digits
        if (value === "" || (/^[6-9]\d*$/.test(value) && /^\d{0,10}$/.test(value))) {
            setFormData((prev) => ({
                ...prev,
                mobileNumber: value,
                user: { ...prev.user, password: value }, // Sync password with mobile number
            }));
        }
    } else if (name === "name") {
        // Only allow alphabets and spaces for name field
        if (value === "" || /^[A-Za-z\s]*$/.test(value)) {
            setFormData((prev) => ({ ...prev, [name]: value }));
        }
    } else if (name === "dno") {
      // Only allow numeric values for dno field, max 4 digits, and first digit should not be 0
      if (
        value === "" ||
        (/^[1-9]\d{0,3}$/.test(value)) // First digit 1-9, followed by up to 3 digits
      ) {
        setFormData((prev) => ({ ...prev, [name]: value }));
      }
    }
     else if (name === "batchName") {
        // Only allow alphabets and spaces for batchName field
        if (value === "" || /^[A-Za-z\s]*$/.test(value)) {
            setFormData((prev) => ({ ...prev, [name]: value }));
        }
    } else if (name === "section") {
        // Only allow alphabets and spaces for section field
        if (value === "" || /^[A-Za-z\s]*$/.test(value)) {
            setFormData((prev) => ({ ...prev, [name]: value }));
        }
    } else {
        setFormData((prev) => ({ ...prev, [name]: value }));
    }
};

  const validateForm = () => {
    let valid = true
    const newErrors = {}

    // Name validation - only alphabets and minimum 5 characters
    if (!formData.name.trim()) {
      newErrors.name = "Name is required."
      valid = false
    } else if (!/^[A-Za-z\s]+$/.test(formData.name)) {
      newErrors.name = "Name should contain only alphabets."
      valid = false
    } else if (formData.name.trim().length < 5) {
      newErrors.name = "Name should have at least 5 characters."
      valid = false
    }

    if (!formData.user.email?.trim()) {
      newErrors.email = "Email is required.";
      valid = false;
    } else if (!/^[A-Za-z][A-Za-z0-9._%+-]*@[A-Za-z0-9.-]+\.(com|in|org)$/i.test(formData.user.email)) {
      newErrors.email = "Enter a valid email starting with a letter and ending in '.com', '.in', or '.org'.";
      valid = false;
    }

    // Password validation
    if (!formData.user.password.trim()) {
      newErrors.password = "Password is required."
      valid = false
    }

    // Mobile number validation
    if (!formData.mobileNumber.trim()) {
      newErrors.mobileNumber = "Mobile number is required."
      valid = false
    } else if (!/^\d{10}$/.test(formData.mobileNumber)) {
      newErrors.mobileNumber = "Mobile number must be exactly 10 digits."
      valid = false
    } else if (/^[0-5]/.test(formData.mobileNumber)) {
      newErrors.mobileNumber = "Mobile number should not start with digits 0-5."
      valid = false
    }

    // Validate that mobile number and password are the same
    if (formData.mobileNumber !== formData.user.password) {
      newErrors.mobileNumber = "Mobile number and password must be the same."
      newErrors.password = "Password and mobile number must be the same."
      valid = false
    }

  // D.No validation - exactly 4 numeric digits and should not start with 0
if (!formData.dno.trim()) {
  newErrors.dno = "D.No is required."
  valid = false
} else if (!/^[1-9]\d{3}$/.test(formData.dno)) {
  newErrors.dno = "D.No must be exactly 4 digits and should not start with 0."
  valid = false
}


    // Department validation
    if (!formData.department.trim()) {
      newErrors.department = "Department is required."
      valid = false
    }

    // Batch name validation - only alphabets, minimum 3 characters
    if (!formData.batchName.trim()) {
      newErrors.batchName = "Batch Name is required."
      valid = false
    } else if (!/^[A-Za-z\s]{3,}$/.test(formData.batchName)) {
      newErrors.batchName = "Batch Name should contain only alphabets with minimum 3 characters."
      valid = false
    }

    // Section validation - only alphabets (optional field)
    if (formData.section.trim() && !/^[A-Za-z\s]+$/.test(formData.section)) {
      newErrors.section = "Section should contain only alphabets."
      valid = false
    }

    setErrors(newErrors)
    return valid
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError("")

    if (!validateForm()) {
      return
    }

    try {
      const studentData = {
        ...formData,
        email: formData.user.email,
        password: formData.user.password,
      }
      if (isEditing) {
        await UserService.updateStudent(editingId, studentData)
        alert("Student updated successfully")
      } else {
        await UserService.registerStudent(studentData)
        alert("Student registered successfully")
      }
      fetchStudents()
      resetForm()
    } catch (error) {
      console.error("Error submitting form:", error)
      setError(error.response?.data?.message || "An error occurred. Please try again.")
    }
  }

  const handleEdit = (student) => {
    console.log(student) // Check the student object
    setIsEditing(true)
    setEditingId(student.id)
    setFormData({
      name: student.name,
      user: {
        email: student.email || "",
        password: student.mobileNumber || "", // Set password to mobile number when editing
      },
      dno: student.dno || "",
      department: student.department || "", // Log if department is correct here
      batchName: student.batchName || "",
      mobileNumber: student.mobileNumber || "",
      section: student.section || "", // Added section field
    })
    console.log(formData.department) // Check if department value is set properly
    setActiveTab("register")
  }

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this student?")) {
      try {
        await UserService.deleteStudent(id)
        alert("Student deleted successfully")
        fetchStudents()
      } catch (error) {
        console.error("Error deleting student:", error)
        setError("Failed to delete student. Please try again.")
      }
    }
  }

  const resetForm = () => {
    setFormData({
      name: "",
      user: { email: "", password: "" },
      dno: "",
      department: "",
      batchName: "",
      mobileNumber: "",
      section: "", // Reset section field
    })
    setIsEditing(false)
    setEditingId(null)
    setError("")
    setErrors({})
  }

  // Updated search function to handle case-insensitive search
  const handleSearch = async (e) => {
    const searchTerm = e.target.value
    setSearchTerm(searchTerm)

    if (searchTerm.trim() === "") {
      setStudents(allStudents)
      setFilteredStudents(allStudents)
    } else {
      try {
        // Try to use the API first
        const results = await UserService.searchStudentsByName(searchTerm)

        // If API doesn't return results or doesn't handle case-insensitive search,
        // perform client-side case-insensitive filtering as a fallback
        if (results.length === 0) {
          const filteredResults = allStudents.filter((student) =>
            student.name.toLowerCase().includes(searchTerm.toLowerCase()),
          )
          setStudents(filteredResults)
          setFilteredStudents(filteredResults)
        } else {
          setStudents(results)
          setFilteredStudents(results)
        }
      } catch (error) {
        console.error("Error searching students:", error)

        // Fallback to client-side filtering if API call fails
        const filteredResults = allStudents.filter((student) =>
          student.name.toLowerCase().includes(searchTerm.toLowerCase()),
        )
        setStudents(filteredResults)
        setFilteredStudents(filteredResults)
        setError("Using local search results.")
      }
    }
  }

  const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (file) {
      handleExcelUpload(file)
    }
  }

  const handleExcelUpload = async (file) => {
    setIsUploading(true);
    setError("");
  
    if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
      setError("Please upload a valid Excel file (.xlsx or .xls)");
      setIsUploading(false);
      return;
    }
  
    const reader = new FileReader();
    reader.onload = async (event) => {
      const data = new Uint8Array(event.target.result);
      const workbook = XLSX.read(data, { type: "array" });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const jsonData = XLSX.utils.sheet_to_json(sheet);
  
      // Validate data before uploading
      const validationErrors = validateStudentExcelData(jsonData);
      if (validationErrors.length > 0) {
        setError("Excel file contains errors:\n" + validationErrors.join("\n"));
        setIsUploading(false);
        return;
      }
  
      const formData = new FormData();
      formData.append("file", file);
      try {
        await UserService.uploadStudentExcel(formData);
        alert("Students uploaded successfully!");
        fetchStudents();
      } catch (error) {
        setError("Error uploading students: " + (error.response?.data?.message || error.message));
      } finally {
        setIsUploading(false);
      }
    };
  
    reader.readAsArrayBuffer(file);
  };
  

  const uploadExcelData = async (file) => {
    try {
      const formData = new FormData()
      formData.append("file", file)

      const response = await UserService.uploadStudentExcel(formData)
      alert(response.message || "Students uploaded successfully")
      fetchStudents()
    } catch (error) {
      console.error("Error uploading Excel file:", error)
      setError(
        error.response?.data?.message ||
          error.message ||
          "Error uploading students. Please check your file and try again.",
      )
    } finally {
      setIsUploading(false)
    }
  }

  const validateStudentExcelData = (data) => {
    const errors = [];
    data.forEach((row, index) => {
      const rowNum = index + 2; // Row number in Excel (considering headers)
  
      if (!row.Name || !/^[A-Za-z\s]+$/.test(row.Name) || row.Name.trim().length < 5) {
        errors.push(`Row ${rowNum}: Invalid Name (Only alphabets, min 5 characters required)`);
      }
  
      if (!row.Email || !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.(com|in|org)$/i.test(row.Email)) {
        errors.push(`Row ${rowNum}: Invalid Email format (must end with .com, .in, or .org)`);
      }
  
      if (!row.Password || !/^\d{10}$/.test(row.Password) || /^[0-5]/.test(row.Password)) {
        errors.push(`Row ${rowNum}: Invalid Password/Mobile Number (Must be 10 digits, should not start with 0-5)`);
      }
  
      if (!row.Dno || !/^\d{4}$/.test(row.Dno)) {
        errors.push(`Row ${rowNum}: Invalid D.No (Must be exactly 4 numeric digits)`);
      }
      
      // Section is optional, but if provided, validate it contains only alphabets
      if (row.Section && !/^[A-Za-z\s]+$/.test(row.Section)) {
        errors.push(`Row ${rowNum}: Invalid Section (Only alphabets allowed)`);
      }
    });
  
    return errors;
  };
  
  const handleFilter = () => {
    const filtered = students.filter((student) => {
      // Check if the selected department matches or if no department is selected
      const departmentMatch = !selectedDepartment || student.department === selectedDepartment

      // Check if the batch input matches (case-insensitive) or if no batch input is provided
      const batchMatch = !batchInput || student.batchName.toLowerCase().includes(batchInput.toLowerCase())

      // Both conditions need to be true to include the student
      return departmentMatch && batchMatch
    })

    // Update the filtered students state
    setFilteredStudents(filtered)
  }

  const handleRegisterClick = () => {
    setActiveTab("register")
    resetForm()
  }

  const handleListClick = () => {
    setActiveTab("list")
    setSelectedDepartment("")
    setBatchInput("")
    setFilteredStudents(students)
  }

  return (
    <div className="student-data-page">
      <AdminNavbar />
      <div className="student-data-container">
        <div className="student-sidebar">
          <h2>Admin Panel</h2>
          <button className="register-button" onClick={handleRegisterClick}>
            Register New Student
          </button>
          <button className="list-button" onClick={handleListClick}>
            List of Students
          </button>
        </div>

        <div className="student-main-content">
          {error && <div className="error-message">{error}</div>}
          {activeTab === "register" && (
            <div className="stud-register-form">
              <h2>{isEditing ? "Edit Student" : "Register New Student"}</h2>
              <form onSubmit={handleSubmit} className="form-group">
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="Name"
                  required
                />
                {errors.name && <span className="error-message">{errors.name}</span>}
                <input
                  type="email"
                  name="email"
                  value={formData.user.email}
                  onChange={handleInputChange}
                  placeholder="Email"
                  required
                />
                {errors.email && <span className="error-message">{errors.email}</span>}
                <input
                  type="tel"
                  name="mobileNumber"
                  value={formData.mobileNumber}
                  onChange={handleInputChange}
                  placeholder="Mobile Number"
                  required
                />
                {errors.mobileNumber && <span className="error-message">{errors.mobileNumber}</span>}
                <input
                  type="password"
                  name="password"
                  value={formData.user.password}
                  onChange={handleInputChange}
                  placeholder="Password"
                  required
                />
                {errors.password && <span className="error-message">{errors.password}</span>}
                
                <input
                  type="text"
                  name="dno"
                  value={formData.dno}
                  onChange={handleInputChange}
                  placeholder="D.No"
                  required
                />
                {errors.dno && <span className="error-message">{errors.dno}</span>}
                <select name="department" value={formData.department} onChange={handleInputChange} required>
                  <option value="">Select Department</option>
                  <option value="Computer Science and Engineering">Computer Science and Engineering</option>
                  <option value="Information Technology">Information Technology</option>
                  <option value="Electronics and Communication Engineering">
                    Electronics and Communication Engineering
                  </option>
                  <option value="Electrical and Electronics Engineering">Electrical and Electronics Engineering</option>
                  <option value="Aeronautical Engineering">Aeronautical Engineering</option>
                  <option value="Mechanical Engineering">Mechanical Engineering</option>
                  <option value="Civil Engineering">Civil Engineering</option>
                </select>
                {errors.department && <span className="error-message">{errors.department}</span>}
                <input
                  type="text"
                  name="batchName"
                  value={formData.batchName}
                  onChange={handleInputChange}
                  placeholder="Batch Name"
                  required
                />
                {errors.batchName && <span className="error-message">{errors.batchName}</span>}
                <input
                  type="text"
                  name="section"
                  value={formData.section}
                  onChange={handleInputChange}
                  placeholder="Section (Optional)"
                />
                {errors.section && <span className="error-message">{errors.section}</span>}
                <div className="form-buttons">
                  <button type="submit" className="register-submit-button">
                    {isEditing ? "Update" : "Register"}
                  </button>
                  <button type="button" onClick={resetForm} className="cancel-button">
                    Cancel
                  </button>
                </div>
              </form>
              <input
                type="file"
                accept=".xlsx, .xls"
                onChange={handleFileChange}
                ref={fileInputRef}
                style={{ display: "none" }}
              />
              <button onClick={() => fileInputRef.current.click()} disabled={isUploading} className="upload-button">
                {isUploading ? "Uploading..." : "Upload Excel"}
              </button>
            </div>
          )}
          {activeTab === "list" && (
            <div className="student-list">
              <h2>Student List</h2>
              <div className="filter-controls">
                <select
                  value={selectedDepartment}
                  onChange={(e) => setSelectedDepartment(e.target.value)}
                  className="department-select"
                >
                  <option value="">Select Department</option>
                  <option value="Computer Science and Engineering">Computer Science and Engineering</option>
                  <option value="Information Technology">Information Technology</option>
                  <option value="Electronics and Communication Engineering">
                    Electronics and Communication Engineering
                  </option>
                  <option value="Electrical and Electronics Engineering">Electrical and Electronics Engineering</option>
                  <option value="Aeronautical Engineering">Aeronautical Engineering</option>
                  <option value="Mechanical Engineering">Mechanical Engineering</option>
                  <option value="Civil Engineering">Civil Engineering</option>
                </select>
                <input
                  type="text"
                  value={batchInput}
                  onChange={(e) => setBatchInput(e.target.value)}
                  placeholder="Filter by Batch Name"
                  className="batch-input"
                />
                <button onClick={handleFilter} className="filter-button">
                  Filter
                </button>
              </div>
              <input
                type="text"
                placeholder="Search students..."
                value={searchTerm}
                onChange={handleSearch}
                className="search-input"
              />
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Mobile Number</th>
                    <th>D.No</th>
                    <th>Department</th>
                    <th>Batch Name</th>
                    <th>Section</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents.map((student) => (
                    <tr key={student.id}>
                      <td>{student.name}</td>
                      <td>{student.email || "N/A"}</td>
                      <td>{student.mobileNumber || "N/A"}</td>
                      <td>{student.dno || "N/A"}</td>
                      <td>{student.department || "N/A"}</td>
                      <td>{student.batchName || "N/A"}</td>
                      <td>{student.section || "N/A"}</td>
                      <td>
                      <div className="stud-list-action-buttons">
                        <button onClick={() => handleEdit(student)} className="stud-edit-button">
                          Edit
                        </button>
                        <button onClick={() => handleDelete(student.id)} className="stud-delete-button">
                          Delete
                        </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default StudentData