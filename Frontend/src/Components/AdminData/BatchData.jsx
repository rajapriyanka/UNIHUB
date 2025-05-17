"use client"

import { useState } from "react"
import AdminNavbar from "../Land/AdminNavbar"
import UserService from "../../Service/UserService"
import "./BatchData.css"

const BatchData = () => {
  const [showForm, setShowForm] = useState(false)
  const [showBatchList, setShowBatchList] = useState(false)
  const [formData, setFormData] = useState({
    batchName: "",
    department: "",
    section: "",
  })
  const [errors, setErrors] = useState({})
  const [batches, setBatches] = useState([])
  const [searchTerm, setSearchTerm] = useState("")
  const [editingBatch, setEditingBatch] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [filterDepartment, setFilterDepartment] = useState("")
  const [filterBatchName, setFilterBatchName] = useState("")

  const departments = [
    "Select Department",
    "Computer Science and Engineering",
    "Information Technology",
    "Electronics and Communication Engineering",
    "Electrical and Electronics Engineering",
    "Mechanical Engineering",
    "Civil Engineering",
    "Aeronautical Engineering",
  ]

  const fetchBatches = async () => {
    setIsLoading(true)
    try {
      const response = await UserService.getAllBatches()
      setBatches(response)
    } catch (error) {
      console.error("Error fetching batches:", error)
      alert("Error fetching batches")
    } finally {
      setIsLoading(false)
    }
  }

  const resetFormData = {
    batchName: "",
    department: "",
    section: "",
  }

  const handleRegisterClick = () => {
    setShowForm(true)
    setShowBatchList(false)
    setEditingBatch(null)
    setFormData(resetFormData)
  }

  const handleListBatchesClick = () => {
    setShowBatchList(true)
    setShowForm(false)
    fetchBatches()
  }

  const handleCancelClick = () => {
    setShowForm(false)
    setEditingBatch(null)
    setFormData(resetFormData)
    setErrors({})
  }

  const validateForm = () => {
    let valid = true
    const newErrors = {}

    if (!formData.batchName.trim()) {
      newErrors.batchName = "Batch name is required."
      valid = false
    } else if (formData.batchName.trim().length < 3) {
      newErrors.batchName = "Batch name should have at least 3 characters."
      valid = false
    } else if (/\d/.test(formData.batchName)) {
      newErrors.batchName = "Batch name should not contain numeric values."
      valid = false
    }
    if (!formData.department || formData.department === "Select Department") {
      newErrors.department = "Department is required."
      valid = false
    }
    if (formData.section && /\d/.test(formData.section)) {
      newErrors.section = "Section should not contain numeric values."
      valid = false
    }

    setErrors(newErrors)
    return valid
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target

    if ((name === "batchName" || name === "section") && /[^a-zA-Z\s]/.test(value)) {
      // If the new value contains numbers or special characters, don't update the state
      return;
    }
    

    // For other fields or if the value is valid, update normally
    setFormData((prevState) => ({
      ...prevState,
      [name]: name === "department" && value === "Select Department" ? "" : value,
    }))
  }

  const handleRegister = async (e) => {
    e.preventDefault()
    if (validateForm()) {
      try {
        if (editingBatch) {
          await UserService.updateBatch(editingBatch.id, formData)
          alert("Batch updated successfully")
        } else {
          await UserService.registerBatch(formData)
          alert("Batch registered successfully")
        }
        setShowForm(false)
        setFormData(resetFormData)
        fetchBatches()
      } catch (error) {
        alert("Error registering batch")
      }
    }
  }

  const handleEdit = (batch) => {
    setEditingBatch(batch)
    setFormData({
      batchName: batch.batchName,
      department: batch.department,
      section: batch.section || "",
    })
    setShowForm(true)
    setShowBatchList(false)
  }

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this batch?")) {
      try {
        await UserService.deleteBatch(id)
        alert("Batch deleted successfully")
        fetchBatches()
      } catch (error) {
        alert("Error deleting batch")
      }
    }
  }

  const handleSearch = async (e) => {
    const term = e.target.value
    setSearchTerm(term)

    if (term.trim() === "") {
      fetchBatches()
    } else {
      try {
        const searchResults = await UserService.searchBatches(term)
        setBatches(searchResults)
      } catch (error) {
        alert("Error searching batches")
      }
    }
  }

  const handleFilter = async () => {
    setIsLoading(true)
    try {
      const response = await UserService.search("batches", {
        department: filterDepartment,
        batchName: filterBatchName,
      })
      setBatches(response)
    } catch (error) {
      console.error("Error filtering batches:", error)
      alert("Error filtering batches")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="batch-data-page">
      <AdminNavbar />
      <div className="batch-data-container">
        <div className="batch-sidebar">
          <h2>Admin Panel</h2>
          <button className="register-button" onClick={handleRegisterClick}>
            Register New Batch
          </button>
          <button className="list-button" onClick={handleListBatchesClick}>
            List of Batches
          </button>
        </div>

        <div className="batch-main-content">
          {showForm && (
            <div className="register-form">
              <h2>{editingBatch ? "Edit Batch" : "Register New Batch"}</h2>
              <form onSubmit={handleRegister}>
                <div className="form-group">
                  <label htmlFor="batchName">Batch Name</label>
                  <input
                    type="text"
                    id="batchName"
                    name="batchName"
                    value={formData.batchName}
                    onChange={handleInputChange}
                    className="form-input"
                  />
                  {errors.batchName && <p className="error-text">{errors.batchName}</p>}
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
                    {departments.map((dept, index) => (
                      <option key={index} value={dept === "Select Department" ? "" : dept}>
                        {dept}
                      </option>
                    ))}
                  </select>
                  {errors.department && <p className="error-text">{errors.department}</p>}
                </div>
                <div className="form-group">
                  <label htmlFor="section">Section (Optional)</label>
                  <input
                    type="text"
                    id="section"
                    name="section"
                    value={formData.section}
                    onChange={handleInputChange}
                    className="form-input"
                  />
                  {errors.section && <p className="error-text">{errors.section}</p>}
                </div>
                <div className="form-actions">
                  <button type="button" className="cancel-button" onClick={handleCancelClick}>
                    Cancel
                  </button>
                  <button type="submit" className="register-submit-button">
                    {editingBatch ? "Update" : "Register"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {showBatchList && (
            <div className="batch-list">
              <h2>Batch List</h2>
              <div className="filter-controls">
                <select
                  className="department-select"
                  value={filterDepartment}
                  onChange={(e) => setFilterDepartment(e.target.value)}
                >
                  <option value="">All Departments</option>
                  {departments.slice(1).map((dept, index) => (
                    <option key={index} value={dept}>
                      {dept}
                    </option>
                  ))}
                </select>
                <input
                  type="text"
                  placeholder="Filter by batch name..."
                  value={filterBatchName}
                  onChange={(e) => setFilterBatchName(e.target.value)}
                  className="batch-name-input"
                />
                <button onClick={handleFilter} className="filter-button">
                  Filter
                </button>
              </div>
              {isLoading ? (
                <p>Loading...</p>
              ) : batches.length === 0 ? (
                <p>No batch data available.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Batch Name</th>
                      <th>Department</th>
                      <th>Section</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {batches.map((batch) => (
                      <tr key={batch.id}>
                        <td>{batch.batchName}</td>
                        <td>{batch.department}</td>
                        <td>{batch.section || "N/A"}</td>
                        <td>
                          <div className="action-buttons">
                          <button onClick={() => handleEdit(batch)} className="batch-edit-button">
                            Edit
                          </button>
                          <button onClick={() => handleDelete(batch.id)} className="batch-delete-button">
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

export default BatchData;

