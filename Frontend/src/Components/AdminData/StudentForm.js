import React from "react"
import PropTypes from "prop-types"

const StudentForm = ({ formData, errors, handleInputChange, handleSubmit, handleCancel, isEditing }) => {
  const departments = [
    "Select Department",
    "Computer Science and Engineering",
    "Electronics and Communication Engineering",
    "Electrical and Electronics Engineering",
    "Mechanical Engineering",
    "Civil Engineering",
   "Information Technology"
  ]

  return (
    <div className="register-form">
      <h2>{isEditing ? "Edit Student" : "Register New Student"}</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Name</label>
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleInputChange}
            className="form-input"
          />
          {errors.name && <p className="error-text">{errors.name}</p>}
        </div>
        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.user.email}
            onChange={handleInputChange}
            className="form-input"
          />
          {errors.email && <p className="error-text">{errors.email}</p>}
        </div>
        <div className="form-group">
          <label htmlFor="password">Password (Mobile Number)</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.user.password}
            onChange={handleInputChange}
            className="form-input"
          />
          {errors.password && <p className="error-text">{errors.password}</p>}
        </div>
        <div className="form-group">
          <label htmlFor="dno">D.No</label>
          <input
            type="text"
            id="dno"
            name="dno"
            value={formData.dno}
            onChange={handleInputChange}
            className="form-input"
          />
          {errors.dno && <p className="error-text">{errors.dno}</p>}
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
              <option key={index} value={dept}>
                {dept}
              </option>
            ))}
          </select>
          {errors.department && <p className="error-text">{errors.department}</p>}
        </div>
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
          <label htmlFor="section">Section (Optional)</label>
          <input
            type="text"
            id="section"
            name="section"
            value={formData.section || ""}
            onChange={handleInputChange}
            className="form-input"
          />
          {errors.section && <p className="error-text">{errors.section}</p>}
        </div>
        <div className="form-actions">
          <button type="button" className="cancel-button" onClick={handleCancel}>
            Cancel
          </button>
          <button type="submit" className="register-submit-button">
            {isEditing ? "Update" : "Register"}
          </button>
        </div>
      </form>
    </div>
  )
}

StudentForm.propTypes = {
  formData: PropTypes.object.isRequired,
  errors: PropTypes.object.isRequired,
  handleInputChange: PropTypes.func.isRequired,
  handleSubmit: PropTypes.func.isRequired,
  handleCancel: PropTypes.func.isRequired,
  isEditing: PropTypes.bool.isRequired,
}

export default StudentForm